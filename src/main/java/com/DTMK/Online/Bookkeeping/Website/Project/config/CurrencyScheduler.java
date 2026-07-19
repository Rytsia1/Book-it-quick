package com.DTMK.Online.Bookkeeping.Website.Project.config;

import com.DTMK.Online.Bookkeeping.Website.Project.service.CurrencySyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily cron job that refreshes the multi-currency exchange rates.
 * <p>
 * Runs at <b>11:00 Asia/Jakarta every day</b> (cron:
 * {@code 0 0 11 * * *}). The {@code zone} is pinned to
 * Asia/Jakarta so the schedule is correct regardless of the JVM's
 * default timezone, and so it matches the MySQL connection's
 * {@code serverTimezone=Asia/Jakarta} already set in
 * {@code application.yml}.
 * <p>
 * The 11:00 slot was chosen for two reasons:
 * <ol>
 *   <li>The exchangerate-api.com free tier publishes a new "daily"
 *       baseline around 00:00 UTC; by 04:00 WIB the data has
 *       propagated to all their edge caches, so 11:00 is a safe
 *       "the rate is definitely fresh" window.</li>
 *   <li>Off-peak from the rest of the app's batch jobs (the
 *       recurring-bill scheduler runs at 00:05 Asia/Shanghai).</li>
 * </ol>
 * <p>
 * The actual work lives in
 * {@link CurrencySyncService#syncRates()}; this class is a thin
 * wrapper that adds structured logging and a defensive try/catch so
 * a network blip doesn't permanently disable the job.
 * <p>
 * Note: this bean only takes effect when {@code @EnableScheduling}
 * is present on
 * {@code OnlineBookkeepingWebsiteProjectApplication} (it already is).
 */
@Component
@RequiredArgsConstructor
public class CurrencyScheduler {

    private static final Logger log = LoggerFactory.getLogger(CurrencyScheduler.class);

    private final CurrencySyncService currencySyncService;

    /**
     * Daily tick at 11:00:00 in Asia/Jakarta.
     * <p>
     * Cron format: {@code second minute hour day month day-of-week}.
     */
    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Jakarta")
    public void runDaily() {
        long start = System.currentTimeMillis();
        log.info("Currency scheduler tick started");
        try {
            int upserted = currencySyncService.syncRates();
            long ms = System.currentTimeMillis() - start;
            log.info("Currency scheduler tick finished: {} rate(s) upserted in {} ms",
                    upserted, ms);
        } catch (Exception e) {
            // Swallow the exception so Spring's scheduler doesn't
            // permanently disable this job. We'll retry tomorrow.
            log.error("Currency scheduler tick failed; will retry at the next scheduled time", e);
        }
    }
}
