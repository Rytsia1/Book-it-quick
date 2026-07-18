package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.BillMapper;
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

    private static final Map<Integer, List<String>> FIXED_CATEGORIES = Map.of(
            1, List.of("Salary", "Bonus", "Freelance", "Investment", "Other Income"),
            0, List.of("Food", "Transport", "Utilities", "Shopping", "Entertainment", "Other Expense")
    );

    // GET /api/bills?userId=1
    @GetMapping
    public ResponseEntity<List<Bill>> getBills(@RequestParam Integer userId) {
        return ResponseEntity.ok(billMapper.findBillsByUserId(userId));
    }

    // POST /api/bills
    @PostMapping
    public ResponseEntity<Map<String, String>> addBill(@RequestBody Bill bill) {
        if (!isValidCategory(bill)) {
            return ResponseEntity.badRequest().body(message("Invalid bill type or category"));
        }
        billMapper.insertBill(bill);
        return ResponseEntity.ok(message("Bill added successfully"));
    }

    // PUT /api/bills/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateBill(@PathVariable Integer id, @RequestBody Bill bill) {
        if (!isValidCategory(bill)) {
            return ResponseEntity.badRequest().body(message("Invalid bill type or category"));
        }
        bill.setId(id);
        billMapper.updateBill(bill);
        return ResponseEntity.ok(message("Bill updated successfully"));
    }

    // DELETE /api/bills/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBill(@PathVariable Integer id) {
        billMapper.deleteBill(id);
        return ResponseEntity.ok(message("Bill deleted successfully"));
    }

    private boolean isValidCategory(Bill bill) {
        List<String> categories = FIXED_CATEGORIES.get(bill.getType());
        return categories != null && categories.contains(bill.getCategory());
    }

    private Map<String, String> message(String text) {
        Map<String, String> response = new HashMap<>();
        response.put("message", text);
        return response;
    }
}
