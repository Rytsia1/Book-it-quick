package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.ExchangeRate;
import com.DTMK.Online.Bookkeeping.Website.Project.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only HTTP entry point for the {@code exchange_rate} table.
 * <p>
 * The table is written exclusively by
 * {@link com.DTMK.Online.Bookkeeping.Website.Project.config.CurrencyScheduler}
 * (a daily cron job that hits exchangerate-api.com). This controller
 * only serves cached rows to the frontend so it can render
 * multi-currency values without burning extra API quota.
 */
@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * Returns every supported rate, sorted alphabetically by
     * {@code currencyCode}. Response shape:
     * <pre>{@code
     * [
     *   { "id": 1, "currencyCode": "CNY", "rate": 7.1234, "lastUpdated": "2026-07-19T11:00:05" },
     *   { "id": 2, "currencyCode": "EUR", "rate": 0.8521, "lastUpdated": "2026-07-19T11:00:05" },
     *   ...
     * ]
     * }</pre>
     * The frontend's {@code composables/useCurrency.js} fetches this
     * once per page load and caches the result.
     */
    @GetMapping
    public ResponseEntity<List<ExchangeRate>> getAll() {
        return ResponseEntity.ok(exchangeRateService.findAll());
    }
}
