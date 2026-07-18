package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Recurring-bill template.
 * <p>
 * One row per recurring bill the user has set up (e.g. "rent on the 1st of
 * every month"). The {@link com.DTMK.Online.Bookkeeping.Website.Project.config.RecurringBillScheduler}
 * scans this table once a day and, for every active template that's due,
 * inserts a fresh {@code t_bill} row and updates {@link #lastRunYearMonth}.
 * <p>
 * Notes on field design:
 * <ul>
 *   <li>{@link #dayOfMonth} is clamped to 1-28 (validated in the service
 *       layer) so the template can always fire, even in February.</li>
 *   <li>{@link #startYearMonth} is a string in {@code YYYY-MM} format.
 *       Templates with a future start are ignored by the scheduler.</li>
 *   <li>{@link #lastRunYearMonth} is the dedup key: once a template has
 *       posted for month X, it won't post again until month X+1, even if
 *       the scheduler runs multiple times in the same month.</li>
 * </ul>
 */
@Data
public class RecurringBill {
    private Integer id;
    private Integer userId;
    private BigDecimal amount;
    private Integer type;                 // 0 = expense, 1 = income
    private String category;
    private String description;
    private Integer dayOfMonth;           // 1-28
    private String startYearMonth;        // YYYY-MM
    private String lastRunYearMonth;      // YYYY-MM or null
    private Integer active;               // 0 = paused, 1 = active
    private LocalDateTime createdAt;
    /**
     * Soft-delete flag. 0 = live, 1 = moved to trash. Filtered out of
     * every read query in {@code RecurringBillMapper} \u2014 critically,
     * the scheduler's hot query {@code findDueTemplates} also filters by
     * {@code is_deleted = 0} so a deleted template never fires again.
     */
    private Integer isDeleted;
}
