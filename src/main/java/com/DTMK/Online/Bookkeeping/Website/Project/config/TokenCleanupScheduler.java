package com.DTMK.Online.Bookkeeping.Website.Project.config;

import com.DTMK.Online.Bookkeeping.Website.Project.mapper.RefreshTokenMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.TokenDenylistMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily cleanup job for the refresh-token and denylist tables.
 * <p>
 * Both tables grow over time as users log in and out: every login
 * inserts a {@code t_refresh_token} row, and every explicit
 * access-token revocation inserts a {@code t_token_denylist} row.
 * Without periodic pruning the tables would accumulate dead rows
 * forever. Each table already carries an {@code expires_at} column
 * that marks when the row becomes meaningless, so the cleanup is a
 * simple {@code DELETE WHERE expires_at < NOW()}.
 * <p>
 * The cron is set to <b>03:00 Asia/Shanghai every day</b> &mdash;
 * intentionally different from {@code RecurringBillScheduler}'s
 * 00:05 slot so a spike in one job can't delay the other. The
 * {@code zone} is pinned for the same reason as in the other
 * scheduler: to be correct regardless of the JVM's default timezone.
 * <p>
 * The try/catch mirrors {@code RecurringBillScheduler}: a single
 * failure must not disable the job going forward.
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenDenylistMapper denylistMapper;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Shanghai")
    public void runDaily() {
        long start = System.currentTimeMillis();
        try {
            int refresh = refreshTokenMapper.deleteExpired();
            int denied  = denylistMapper.deleteExpired();
            long ms = System.currentTimeMillis() - start;
            log.info("Token cleanup finished: {} refresh-token row(s), {} denylist row(s) removed in {} ms",
                    refresh, denied, ms);
        } catch (Exception e) {
            // Swallow so Spring's scheduler doesn't permanently disable
            // the job; we'll retry tomorrow.
            log.error("Token cleanup failed; will retry at the next scheduled time", e);
        }
    }
}
