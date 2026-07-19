package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row per supported target currency, expressing the rate at which
 * 1 USD converts to that currency.
 * <p>
 * The table is populated by
 * {@link com.DTMK.Online.Bookkeeping.Website.Project.config.CurrencyScheduler},
 * which hits the exchangerate-api.com endpoint once a day at 11:00 WIB
 * and upserts every supported code (IDR, CNY, RUB, EUR, GBP, SAR, JPY,
 * KRW, PHP, INR, THB, MYR, TWD, HKD).
 * <p>
 * Notes on field design:
 * <ul>
 *   <li>{@link #rate} is a {@link BigDecimal} so monetary precision is
 *       preserved end-to-end (the API returns doubles; the mapper stores
 *       a DECIMAL(20,8) so a value like 15842.31250000 is lossless).</li>
 *   <li>{@link #lastUpdated} is stamped by the service layer, not by
 *       the DB, so it always reflects the moment of the most recent
 *       successful sync (the API's "time_last_update_unix" is ignored
 *       on purpose — we want the wall-clock at sync time, in Jakarta
 *       time, to match the rest of the app).</li>
 * </ul>
 */
@Data
public class ExchangeRate {
    private Integer id;
    private String currencyCode;        // ISO 4217, e.g. "IDR"
    private BigDecimal rate;            // 1 USD = rate * target_currency
    private LocalDateTime lastUpdated;
}
