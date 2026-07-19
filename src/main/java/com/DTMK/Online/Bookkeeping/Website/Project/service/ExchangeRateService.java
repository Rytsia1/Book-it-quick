package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.ExchangeRate;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.ExchangeRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only view of the {@code exchange_rate} table.
 * <p>
 * The heavy lifting (fetching from
 * {@code https://v6.exchangerate-api.com/.../latest/USD} and upserting
 * each supported code) lives in
 * {@link com.DTMK.Online.Bookkeeping.Website.Project.service.CurrencySyncService},
 * which is invoked by
 * {@link com.DTMK.Online.Bookkeeping.Website.Project.config.CurrencyScheduler}
 * once a day.
 * <p>
 * This class exists so the controller layer has a single, conventional
 * entry point (mirroring the rest of the project, e.g.
 * {@code BillService}, {@code CategoryService}). It also gives us a
 * natural place to add caching, fallback rates, or per-user overrides
 * later without touching the controller.
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateMapper exchangeRateMapper;

    /**
     * @return every row in the {@code exchange_rate} table, ordered
     *         alphabetically by {@code currency_code} (enforced in
     *         the mapper). The list is the same shape the frontend
     *         caches in {@code composables/useCurrency.js} to power
     *         the dropdown + on-the-fly conversions.
     */
    public List<ExchangeRate> findAll() {
        return exchangeRateMapper.findAll();
    }
}
