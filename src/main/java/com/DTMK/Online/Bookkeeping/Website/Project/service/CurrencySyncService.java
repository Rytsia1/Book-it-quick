package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.ExchangeRate;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.ExchangeRateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pulls fresh USD-based exchange rates from
 * {@code https://v6.exchangerate-api.com/v6/.../latest/USD} and upserts
 * every supported target currency into the {@code exchange_rate} table.
 * <p>
 * Called once a day at 11:00 Asia/Jakarta by
 * {@link com.DTMK.Online.Bookkeeping.Website.Project.config.CurrencyScheduler}.
 * <p>
 * <b>Quota / latency:</b> the upstream plan only allows a small number
 * of monthly requests, so the API is hit at most once per day and the
 * result is cached in the DB for everything else (frontend reads,
 * reports, etc.) to consume.
 * <p>
 * <b>What we store:</b> only the codes listed in {@link #SUPPORTED_CODES}.
 * The API returns ~160 currencies; storing the whole set would inflate
 * the table for zero benefit, and the supported set is exactly the set
 * the rest of the app cares about.
 */
@Service
@RequiredArgsConstructor
public class CurrencySyncService {

    private static final Logger log = LoggerFactory.getLogger(CurrencySyncService.class);

    /**
     * Free-tier exchangerate-api.com endpoint. The API key is the
     * project-wide one configured for this backend; rotating it just
     * means changing this constant.
     */
    private static final String API_URL =
            "https://v6.exchangerate-api.com/v6/12e0532d0fbb84b61cd80e2d/latest/USD";

    /**
     * The set of target currencies we care about. Order is preserved
     * (LinkedHashSet) only so logs are deterministic; lookup is O(1)
     * either way.
     */
    private static final Set<String> SUPPORTED_CODES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "IDR", "CNY", "RUB", "EUR", "GBP",
                    "SAR", "JPY", "KRW", "PHP", "INR",
                    "THB", "MYR", "TWD", "HKD"
            ))
    );

    private final RestTemplate restTemplate;
    private final ExchangeRateMapper exchangeRateMapper;
    private final ObjectMapper objectMapper;

    /**
     * End-to-end sync. Returns the number of rows successfully upserted
     * (insert + update). Throws on hard failure (network down, malformed
     * JSON) so the scheduler can log it; per-currency failures are
     * caught and logged so one bad row doesn't poison the batch.
     *
     * @throws JsonProcessingException if the upstream response is not valid JSON
     */
    public int syncRates() throws JsonProcessingException {
        long start = System.currentTimeMillis();
        log.info("Currency sync started ({} target codes)", SUPPORTED_CODES.size());

        // 1) Fetch the raw JSON body as a String. Doing it manually
        //    (instead of letting RestTemplate bind to a DTO) lets us
        //    pull only the `conversion_rates` sub-object — the rest of
        //    the payload (result, time_last_update_unix, etc.) is noise.
        String body = restTemplate.getForObject(API_URL, String.class);
        if (body == null || body.isEmpty()) {
            throw new IllegalStateException("Empty response from " + API_URL);
        }

        // 2) Parse just the conversion_rates sub-tree.
        @SuppressWarnings("unchecked")
        Map<String, Object> conversionRates = objectMapper.readValue(
                body, Map.class).get("conversion_rates") instanceof Map
                        ? (Map<String, Object>) objectMapper.readValue(body, Map.class).get("conversion_rates")
                        : Collections.emptyMap();

        if (conversionRates.isEmpty()) {
            throw new IllegalStateException(
                    "conversion_rates missing or empty in response: " + body);
        }

        LocalDateTime now = LocalDateTime.now();
        int upserted = 0;

        // 3) For every supported code, decide insert vs update.
        for (String code : SUPPORTED_CODES) {
            Object raw = conversionRates.get(code);
            if (raw == null) {
                // The API didn't return this code today. Skip rather
                // than fail the whole batch; the row (if any) keeps its
                // previous rate and last_updated.
                log.warn("Currency {} not present in API response; leaving existing row untouched", code);
                continue;
            }
            BigDecimal rate = toBigDecimal(raw);
            if (rate == null) {
                log.warn("Currency {} had a non-numeric rate ({}); skipping", code, raw);
                continue;
            }
            try {
                upserted += upsertOne(code, rate, now) ? 1 : 0;
            } catch (Exception ex) {
                // Per-row transaction (see upsertOne) already rolled
                // back. The other codes are unaffected; the next
                // scheduler tick will retry this one.
                log.error("Failed to upsert currency {}", code, ex);
            }
        }

        long ms = System.currentTimeMillis() - start;
        log.info("Currency sync finished: {} of {} codes upserted in {} ms",
                upserted, SUPPORTED_CODES.size(), ms);
        return upserted;
    }

    /**
     * Single-row upsert, wrapped in {@code REQUIRES_NEW} so a failure
     * on this code doesn't poison the rest of the batch. Returns
     * {@code true} when a row was inserted or updated.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean upsertOne(String code, BigDecimal rate, LocalDateTime now) {
        ExchangeRate existing = exchangeRateMapper.findByCurrencyCode(code);
        ExchangeRate row = new ExchangeRate();
        row.setCurrencyCode(code);
        row.setRate(rate);
        row.setLastUpdated(now);
        if (existing == null) {
            exchangeRateMapper.insert(row);
            log.debug("Inserted exchange_rate {} = {}", code, rate);
        } else {
            exchangeRateMapper.update(row);
            log.debug("Updated exchange_rate {} = {}", code, rate);
        }
        return true;
    }

    /**
     * Coerce whatever Jackson gave us (typically {@link Double}, but
     * could be {@link Integer} / {@link Long} / {@link String} on a
     * weird API day) into a {@link BigDecimal} without losing
     * precision via the double fast-path.
     */
    private static BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof BigDecimal) {
            return (BigDecimal) raw;
        }
        if (raw instanceof Number) {
            // Use toString() (not doubleValue()) so e.g. 15842.3125
            // does not become 15842.31249999... on the way into the DB.
            return new BigDecimal(raw.toString());
        }
        if (raw instanceof String) {
            try {
                return new BigDecimal((String) raw);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
