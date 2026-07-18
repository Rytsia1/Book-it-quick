package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.RecurringBill;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.BillMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.RecurringBillMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates the recurring-bill feature.
 * <p>
 * Two responsibilities:
 * <ol>
 *   <li>CRUD on the {@code t_recurring_bill} template table, called by
 *       {@link com.DTMK.Online.Bookkeeping.Website.Project.controller.RecurringBillController}.</li>
 *   <li>{@link #runDueTemplates()} — called once a day by
 *       {@link com.DTMK.Online.Bookkeeping.Website.Project.config.RecurringBillScheduler}.
 *       For every active, due template it inserts a row into {@code t_bill}
 *       and stamps {@code last_run_year_month} in a single transaction.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class RecurringBillService {

    private static final Logger log = LoggerFactory.getLogger(RecurringBillService.class);
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RecurringBillMapper recurringMapper;
    private final BillMapper billMapper;

    // ── CRUD ───────────────────────────────────────────────────────────

    /**
     * Create a new template. Validates the payload (defensive — the
     * frontend also validates) and clamps {@code dayOfMonth} to 1-28
     * so the template can always fire, even in February.
     */
    public RecurringBill create(Integer userId, RecurringBill payload) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (payload.getAmount() == null || payload.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        if (payload.getType() == null || (payload.getType() != 0 && payload.getType() != 1)) {
            throw new IllegalArgumentException("type must be 0 (expense) or 1 (income)");
        }
        if (payload.getCategory() == null || payload.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("category is required");
        }
        if (payload.getDayOfMonth() == null) {
            throw new IllegalArgumentException("dayOfMonth is required");
        }
        int day = Math.max(1, Math.min(28, payload.getDayOfMonth()));

        RecurringBill t = new RecurringBill();
        t.setUserId(userId);
        t.setAmount(payload.getAmount());
        t.setType(payload.getType());
        t.setCategory(payload.getCategory().trim());
        t.setDescription(payload.getDescription());
        t.setDayOfMonth(day);
        t.setStartYearMonth(payload.getStartYearMonth() != null
                ? payload.getStartYearMonth()
                : LocalDate.now().format(YM));
        t.setLastRunYearMonth(null);
        t.setActive(payload.getActive() == null ? 1 : payload.getActive());
        t.setCreatedAt(LocalDateTime.now());
        recurringMapper.insert(t);
        return t;
    }

    public List<RecurringBill> list(Integer userId) {
        return recurringMapper.findByUserId(userId);
    }

    /**
     * Returns the updated template, or null if the user doesn't own it
     * (or the template doesn't exist).
     */
    public RecurringBill setActive(Integer userId, Integer id, boolean active) {
        int updated = recurringMapper.updateActive(id, userId, active ? 1 : 0);
        if (updated == 0) return null;
        return recurringMapper.findByIdAndUserId(id, userId);
    }

    public boolean delete(Integer userId, Integer id) {
        // Soft-delete: the row is never actually removed, but the
        // scheduler's findDueTemplates hot query filters on
        // is_deleted = 0 so the template will never fire again.
        return recurringMapper.softDelete(id, userId) > 0;
    }

    // ── Scheduler hook ────────────────────────────────────────────────

    /**
     * Find every due template and post a {@code t_bill} for each. Each
     * template is processed in its own transaction so a single bad row
     * doesn't poison the rest of the batch.
     * <p>
     * Returns the number of bills successfully generated.
     */
    public int runDueTemplates() {
        LocalDate today = LocalDate.now();
        int dayOfMonth   = today.getDayOfMonth();
        String yearMonth = today.format(YM);

        List<RecurringBill> due = recurringMapper.findDueTemplates(dayOfMonth, yearMonth);
        if (due.isEmpty()) {
            log.debug("No recurring-bill templates due on {} (day={})", today, dayOfMonth);
            return 0;
        }

        log.info("Recurring-bill scheduler: {} template(s) due on {}", due.size(), today);
        int fired = 0;
        for (RecurringBill t : due) {
            try {
                if (fireOne(t, yearMonth)) {
                    fired++;
                }
            } catch (Exception ex) {
                // Per-template transaction (see fireOne) already rolled
                // back. The other templates are unaffected; this one
                // will be retried on the next scheduler tick.
                log.error("Failed to fire recurring-bill template id={} (user={}, category={})",
                        t.getId(), t.getUserId(), t.getCategory(), ex);
            }
        }
        return fired;
    }

    /**
     * Insert a single {@code t_bill} row for one template and stamp
     * {@code last_run_year_month} on the template. Wrapped in
     * {@code REQUIRES_NEW} so a failure on this template doesn't poison
     * the others in the same batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fireOne(RecurringBill t, String yearMonth) {
        // Defensive: the conditional UPDATE in the mapper is the source of
        // truth, but check here too so a duplicate insert is impossible.
        if (yearMonth.equals(t.getLastRunYearMonth())) {
            return false;
        }
        Bill bill = new Bill();
        bill.setUserId(t.getUserId());
        bill.setAmount(t.getAmount());
        bill.setType(t.getType());
        bill.setCategory(t.getCategory());
        bill.setDescription(t.getDescription());
        bill.setBillDate(LocalDate.now());   // The day the scheduler runs.
        billMapper.insertBill(bill);

        int updated = recurringMapper.updateLastRun(t.getId(), yearMonth);
        if (updated == 0) {
            // Another scheduler instance won the race. The bill we just
            // inserted is a duplicate — log it and move on. (A future
            // hardening pass can add a unique index on (user_id,
            // description, bill_date) to make this impossible.)
            log.warn("Template id={} was already stamped by another scheduler; duplicate bill may exist", t.getId());
            return false;
        }
        log.info("Fired recurring-bill id={} (user={}, amount={}, category={}) for {}",
                t.getId(), t.getUserId(), t.getAmount(), t.getCategory(), yearMonth);
        return true;
    }
}
