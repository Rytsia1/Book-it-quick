package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Map<String, String>> setMonthlyBudget(@RequestBody Map<String, Object> payload) {
        Integer userId = Integer.valueOf(payload.get("userId").toString());
        BigDecimal monthlyBudget = new BigDecimal(payload.get("monthlyBudget").toString());
        userMapper.updateMonthlyBudget(userId, monthlyBudget);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Monthly budget saved successfully");
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
