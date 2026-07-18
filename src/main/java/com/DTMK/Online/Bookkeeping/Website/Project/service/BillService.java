package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.dto.MonthlyStatsDTO;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.Bill;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.BillMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillMapper billMapper;
    private final UserMapper userMapper;

    public List<Bill> getBills(Integer userId) {
        return billMapper.findBillsByUserId(userId);
    }

    public String addBill(Bill bill) {
        billMapper.insertBill(bill);
        return "Success: Financial record added successfully!";
    }

    public String deleteBill(Integer id) {
        billMapper.deleteBill(id);
        return "Success: Financial record deleted successfully!";
    }

    public List<com.DTMK.Online.Bookkeeping.Website.Project.dto.CategoryStatDTO> getExpenseByCategory(Integer userId, int month, int year) {
        return billMapper.getExpenseByCategory(userId, month, year);
    }

    public MonthlyStatsDTO getMonthlyStats(Integer userId, int month, int year) {
        BigDecimal income = billMapper.calculateMonthlyIncome(userId, month, year);
        BigDecimal expense = billMapper.calculateMonthlyExpense(userId, month, year);
        BigDecimal balance = income.subtract(expense); // Income minus expenses.

        MonthlyStatsDTO stats = new MonthlyStatsDTO();
        stats.setTotalIncome(income);
        stats.setTotalExpense(expense);
        stats.setBalance(balance);

        // Budget alert logic
        User user = userMapper.findById(userId);
        BigDecimal monthlyBudget = (user != null) ? user.getMonthlyBudget() : null;
        stats.setMonthlyBudget(monthlyBudget);

        if (monthlyBudget != null && monthlyBudget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal budgetUsedPercent = expense
                    .divide(monthlyBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            stats.setBudgetUsedPercent(budgetUsedPercent);
            stats.setBudgetWarning(budgetUsedPercent.compareTo(BigDecimal.valueOf(80)) >= 0);
            stats.setBudgetExceeded(budgetUsedPercent.compareTo(BigDecimal.valueOf(100)) >= 0);
        }

        return stats;
    }

    public List<Bill> getBillsByCategory(Integer userId, String category, int month, int year) {
        return billMapper.findBillsByCategoryAndMonth(userId, category, month, year);
    }

    public List<Bill> getBillsByType(Integer userId, int type, int month, int year) {
        return billMapper.findBillsByTypeAndMonth(userId, type, month, year);
    }

}
