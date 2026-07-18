package com.DTMK.Online.Bookkeeping.Website.Project.config;

import com.DTMK.Online.Bookkeeping.Website.Project.service.RecurringBillService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily cron job that fires the recurring-bill feature.
 * <p>
 * Runs at <b>00:05 Asia/Shanghai every day</b> (cron: {@code 0 5 0 * * *}).
 * The 5-minute offset avoids the typical 00:00 spike when other batch
 * jobs and reports fire. The {@code zone} is pinned to Asia/Shanghai
 * so the schedule is correct regardless of the JVM's default timezone
 * (the same timezone the rest of the app uses, per the MySQL connection
 * string and the project convention).
 * <p>
 * The actual work lives in
 * {@link RecurringBillService#runDueTemplates()}; this class is a thin
 * wrapper that adds structured logging and a defensive try/catch so a
 * single misbehaving template cannot stop the scheduler from running
 * tomorrow.
 * <p>
 * Note: this bean only takes effect when
 * {@code @EnableScheduling} is present on
 * {@code OnlineBookkeepingWebsiteProjectApplication}.
 */
@Component
@RequiredArgsConstructor
public class RecurringBillScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringBillScheduler.class);

    private final RecurringBillService recurringBillService;

    /**
     * Daily tick at 00:05 in the project's local timezone.
     * <p>
     * Cron format: {@code second minute hour day month day-of-week}.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    public void runDaily() {
        long start = System.currentTimeMillis();
        log.info("Recurring-bill scheduler tick started");
        try {
            int fired = recurringBillService.runDueTemplates();
            long ms = System.currentTimeMillis() - start;
            log.info("Recurring-bill scheduler tick finished: {} bill(s) generated in {} ms", fired, ms);
        } catch (Exception e) {
            // Swallow the exception so Spring's scheduler doesn't
            // permanently disable this job. We'll retry tomorrow.
            log.error("Recurring-bill scheduler tick failed; will retry at the next scheduled time", e);
        }
    }
}
