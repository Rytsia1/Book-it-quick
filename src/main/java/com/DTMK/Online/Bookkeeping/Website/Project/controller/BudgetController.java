package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final UserMapper userMapper;

    // PUT /api/budget - save the user's monthly budget target.
    @PutMapping
    public ResponseEntity<Map<String, Object>> setMonthlyBudget(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        // 1) Validate payload contains the required keys.
        if (payload == null || !payload.containsKey("userId") || !payload.containsKey("monthlyBudget")) {
            response.put("success", false);
            response.put("message", "Missing required fields: userId and monthlyBudget are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 2) Safely parse userId (handles Number, String, null).
        Integer userId;
        try {
            Object userIdRaw = payload.get("userId");
            if (userIdRaw == null) {
                response.put("success", false);
                response.put("message", "userId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            userId = Integer.valueOf(userIdRaw.toString());
        } catch (NumberFormatException ex) {
            response.put("success", false);
            response.put("message", "Invalid userId format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 3) Safely parse monthlyBudget (handles Number, String, null).
        BigDecimal monthlyBudget;
        try {
            Object budgetRaw = payload.get("monthlyBudget");
            if (budgetRaw == null) {
                response.put("success", false);
                response.put("message", "monthlyBudget is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            monthlyBudget = new BigDecimal(budgetRaw.toString());
            // Reject negative budget values explicitly.
            if (monthlyBudget.compareTo(BigDecimal.ZERO) < 0) {
                response.put("success", false);
                response.put("message", "monthlyBudget cannot be negative");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (NumberFormatException ex) {
            response.put("success", false);
            response.put("message", "Invalid monthlyBudget format");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 4) Ensure the user actually exists before updating.
        User existing = userMapper.findById(userId);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 5) Persist the update.
        try {
            userMapper.updateMonthlyBudget(userId, monthlyBudget);
        } catch (Exception ex) {
            // Likely causes: missing 'monthly_budget' column (migration not run), DB connection error, etc.
            response.put("success", false);
            response.put("message", "Failed to save budget: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        response.put("success", true);
        response.put("message", "Monthly budget saved successfully");
        response.put("monthlyBudget", monthlyBudget);
        return ResponseEntity.ok(response);
    }

    // GET /api/budget/{userId} - retrieve the saved budget.
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getMonthlyBudget(@PathVariable Integer userId) {
        User user = userMapper.findById(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("monthlyBudget", user != null ? user.getMonthlyBudget() : null);
        return ResponseEntity.ok(response);
    }
}
