package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.dto.MonthlyStatsDTO;
import com.DTMK.Online.Bookkeeping.Website.Project.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final BillService billService;

    // GET /api/stats/summary?userId=1&month=7&year=2026
    @GetMapping("/summary")
    public ResponseEntity<MonthlyStatsDTO> getSummary(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getMonthlyStats(userId, month, year));
    }

    // GET /api/stats/categories?userId=1&month=7&year=2026
    @GetMapping("/categories")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO>> getCategories(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getExpenseByCategory(userId, month, year));
    }

    // GET /api/stats/income-categories?userId=1&month=7&year=2026
    @GetMapping("/income-categories")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO>> getIncomeCategories(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getIncomeByCategory(userId, month, year));
    }

    // GET /api/stats/transactions?userId=1&category=Food&type=0&month=7&year=2026
    @GetMapping("/transactions")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill>> getTransactionsByCategory(
            @RequestParam Integer userId,
            @RequestParam String category,
            @RequestParam(required = false, defaultValue = "0") int type,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getBillsByCategory(userId, category, type, month, year));
    }

    // GET /api/stats/bills-by-type?userId=1&type=0&month=7&year=2026
    @GetMapping("/bills-by-type")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill>> getBillsByType(
            @RequestParam Integer userId,
            @RequestParam int type,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getBillsByType(userId, type, month, year));
    }

    // GET /api/stats/daily?userId=1&month=7&year=2026
    // Returns per-day income and expense totals for the line chart.
    @GetMapping("/daily")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.dto.DailyStatDTO>> getDailyStats(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getDailyStats(userId, month, year));
    }

    // GET /api/stats/all-bills?userId=1&month=7&year=2026
    // Returns all transactions (income + expense) for a month (used by the
    // "Remaining Balance" summary card to show every transaction that contributed
    // to the balance).
    @GetMapping("/all-bills")
    public ResponseEntity<java.util.List<com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill>> getAllBills(
            @RequestParam Integer userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(billService.getAllBillsByMonth(userId, month, year));
    }
}
