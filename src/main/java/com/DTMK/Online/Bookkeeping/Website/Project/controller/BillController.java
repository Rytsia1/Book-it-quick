package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.BillMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills") // Uses the plural route expected by the frontend.
@RequiredArgsConstructor
public class BillController {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    /**
     * Default categories that every new user can use out of the box.
     * These are the "system" categories. They are NOT stored in {@code t_category}
     * because they apply to all users regardless of the row count.
     */
    private static final Map<Integer, List<String>> FIXED_CATEGORIES = Map.of(
            1, List.of("Salary", "Bonus", "Freelance", "Investment", "Loan", "Debt", "Other Income"),
            0, List.of("Food", "Transport", "Utilities", "Shopping", "Entertainment", "Health",
                    "Education", "Rent", "Other Expense")
    );

    // GET /api/bills?userId=1
    @GetMapping
    public ResponseEntity<List<Bill>> getBills(@RequestParam Integer userId) {
        return ResponseEntity.ok(billMapper.findBillsByUserId(userId));
    }

    // POST /api/bills
    @PostMapping
    public ResponseEntity<Map<String, String>> addBill(@RequestBody Bill bill) {
        if (bill.getUserId() == null || bill.getType() == null || bill.getCategory() == null) {
            return ResponseEntity.badRequest().body(message("Invalid bill payload: userId, type, and category are required"));
        }
        String normalized = bill.getCategory().trim();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest().body(message("Category cannot be empty"));
        }
        // Reject if the same name is reserved for the *opposite* transaction
        // type (e.g. "Salary" is a default income category, so it cannot be
        // used as an expense category). This prevents users from accidentally
        // entering income categories under expenses or vice versa.
        if (isReservedForOtherType(bill.getType(), normalized)) {
            String section = bill.getType() == 1 ? "expense" : "income";
            return ResponseEntity.badRequest().body(message(
                "\"" + normalized + "\" is a " + section + " category. Please pick a " + section + " category instead."));
        }
        // Also reject if the user already has a custom category of the same
        // name in the opposite type.
        if (hasCustomCategoryInOtherType(bill.getUserId(), bill.getType(), normalized)) {
            String section = bill.getType() == 1 ? "expense" : "income";
            return ResponseEntity.badRequest().body(message(
                "\"" + normalized + "\" already exists as a " + section + " category. Please pick a " + section + " category instead."));
        }
        // The category is accepted if it is either:
        //   1) one of the fixed system categories, OR
        //   2) already persisted in t_category for this user and type.
        // If it does not match either, persist it as a new custom category on the fly.
        if (!isKnownCategory(bill.getUserId(), bill.getType(), normalized)) {
            Category created = new Category();
            created.setUserId(bill.getUserId());
            created.setType(bill.getType());
            created.setName(normalized);
            categoryMapper.insertCategory(created);
        }
        bill.setCategory(normalized);
        billMapper.insertBill(bill);
        return ResponseEntity.ok(message("Bill added successfully"));
    }

    // PUT /api/bills/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateBill(@PathVariable Integer id, @RequestBody Bill bill) {
        if (bill.getType() == null || bill.getCategory() == null) {
            return ResponseEntity.badRequest().body(message("Invalid bill payload: type and category are required"));
        }
        String normalized = bill.getCategory().trim();
        if (normalized.isEmpty()) {
            return ResponseEntity.badRequest().body(message("Category cannot be empty"));
        }
        // Same cross-type check as POST: prevent the user from saving an
        // income-category name under expenses (or vice versa).
        if (isReservedForOtherType(bill.getType(), normalized)) {
            String section = bill.getType() == 1 ? "expense" : "income";
            return ResponseEntity.badRequest().body(message(
                "\"" + normalized + "\" is a " + section + " category. Please pick a " + section + " category instead."));
        }
        Integer userId = bill.getUserId();
        if (userId == null) {
            // The frontend usually sends the userId, but if not, fall back to the existing record.
            Bill existing = billMapper.findById(id);
            if (existing != null) userId = existing.getUserId();
        }
        if (userId != null) {
            if (hasCustomCategoryInOtherType(userId, bill.getType(), normalized)) {
                String section = bill.getType() == 1 ? "expense" : "income";
                return ResponseEntity.badRequest().body(message(
                    "\"" + normalized + "\" already exists as a " + section + " category. Please pick a " + section + " category instead."));
            }
            if (!isKnownCategory(userId, bill.getType(), normalized)) {
                Category created = new Category();
                created.setUserId(userId);
                created.setType(bill.getType());
                created.setName(normalized);
                categoryMapper.insertCategory(created);
            }
        }
        bill.setId(id);
        bill.setCategory(normalized);
        billMapper.updateBill(bill);
        return ResponseEntity.ok(message("Bill updated successfully"));
    }

    // DELETE /api/bills/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBill(@PathVariable Integer id) {
        billMapper.deleteBill(id);
        return ResponseEntity.ok(message("Bill deleted successfully"));
    }

    /**
     * A category is considered "known" for the given user/type if it matches
     * a fixed system category OR is already stored in {@code t_category}.
     * Comparison is case-insensitive to avoid duplicates like "Food" vs "food".
     */
    private boolean isKnownCategory(Integer userId, Integer type, String name) {
        if (type == null || name == null) return false;
        List<String> fixed = FIXED_CATEGORIES.get(type);
        if (fixed != null) {
            for (String f : fixed) {
                if (f.equalsIgnoreCase(name)) return true;
            }
        }
        if (userId == null) return false;
        List<Category> custom = categoryMapper.findCategoriesByUserId(userId);
        if (custom == null) return false;
        for (Category c : custom) {
            if (type.equals(c.getType()) && c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when the given category name is reserved for the *opposite*
     * transaction type as a system default (e.g. "Salary" is a default income
     * category, so it cannot be used as an expense category).
     */
    private boolean isReservedForOtherType(Integer currentType, String name) {
        if (currentType == null || name == null) return false;
        int other = currentType == 1 ? 0 : 1;
        List<String> fixed = FIXED_CATEGORIES.get(other);
        if (fixed == null) return false;
        for (String f : fixed) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Returns true when the user already has a *custom* category of the given
     * name in the opposite transaction type. This is the user-defined
     * counterpart of {@link #isReservedForOtherType}.
     */
    private boolean hasCustomCategoryInOtherType(Integer userId, Integer currentType, String name) {
        if (userId == null || currentType == null || name == null) return false;
        int other = currentType == 1 ? 0 : 1;
        List<Category> custom = categoryMapper.findCategoriesByUserId(userId);
        if (custom == null) return false;
        for (Category c : custom) {
            if (other == c.getType() && c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> message(String text) {
        Map<String, String> response = new HashMap<>();
        response.put("message", text);
        return response;
    }
}
