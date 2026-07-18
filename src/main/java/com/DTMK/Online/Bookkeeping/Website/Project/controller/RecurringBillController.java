package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.RecurringBill;
import com.DTMK.Online.Bookkeeping.Website.Project.service.RecurringBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for managing recurring-bill templates.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code POST   /api/recurring-bills}                — create a new template</li>
 *   <li>{@code GET    /api/recurring-bills?userId=…}       — list the user's templates</li>
 *   <li>{@code PATCH  /api/recurring-bills/{id}/pause}     — pause a template</li>
 *   <li>{@code PATCH  /api/recurring-bills/{id}/resume}    — resume a paused template</li>
 *   <li>{@code DELETE /api/recurring-bills/{id}?userId=…} — delete a template</li>
 * </ul>
 * <p>
 * All endpoints return a uniform {@code { success, message, data }} envelope
 * for happy paths and let the {@code GlobalExceptionHandler} turn
 * {@link IllegalArgumentException} into a clean 400 for invalid payloads.
 */
@RestController
@RequestMapping("/api/recurring-bills")
@RequiredArgsConstructor
public class RecurringBillController {

    private final RecurringBillService recurringBillService;

    // POST /api/recurring-bills
    // Body: { userId, amount, type, category, description, dayOfMonth, startYearMonth? }
    @PostMapping
    public ResponseEntity<RecurringBill> create(@RequestBody Map<String, Object> body) {
        Integer userId = asInt(body.get("userId"));
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        RecurringBill payload = new RecurringBill();
        payload.setAmount(asBigDecimal(body.get("amount")));
        payload.setType(asInt(body.get("type")));
        payload.setCategory(asString(body.get("category")));
        payload.setDescription(asString(body.get("description")));
        payload.setDayOfMonth(asInt(body.get("dayOfMonth")));
        payload.setStartYearMonth(asString(body.get("startYearMonth")));
        payload.setActive(asInt(body.get("active")));  // optional
        RecurringBill created = recurringBillService.create(userId, payload);
        return ResponseEntity.ok(created);
    }

    // GET /api/recurring-bills?userId=1
    @GetMapping
    public ResponseEntity<List<RecurringBill>> list(@RequestParam Integer userId) {
        return ResponseEntity.ok(recurringBillService.list(userId));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringBill> pause(@PathVariable Integer id, @RequestParam Integer userId) {
        RecurringBill updated = recurringBillService.setActive(userId, id, false);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringBill> resume(@PathVariable Integer id, @RequestParam Integer userId) {
        RecurringBill updated = recurringBillService.setActive(userId, id, true);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/recurring-bills/{id}?userId=...
    // Soft delete: marks the template as trashed. The row is never
    // actually removed, so the next cron tick simply skips it (the
    // findDueTemplates hot query now filters on is_deleted = 0).
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id, @RequestParam Integer userId) {
        boolean deleted = recurringBillService.delete(userId, id);
        Map<String, Object> body = new HashMap<>();
        if (deleted) {
            body.put("success", true);
            body.put("message", "Recurring bill moved to trash");
            return ResponseEntity.ok(body);
        }
        body.put("success", false);
        body.put("message", "Recurring bill not found or not owned by this user");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── Tiny body-extraction helpers ──────────────────────────────────
    // We accept loose Map<String,Object> so the frontend can send numbers
    // as numbers or strings; this mirrors BudgetController's setMonthlyBudget.

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString().trim()); }
        catch (NumberFormatException ex) { return null; }
    }

    private static java.math.BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return new java.math.BigDecimal(n.toString());
        try { return new java.math.BigDecimal(o.toString().trim()); }
        catch (NumberFormatException ex) { return null; }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
