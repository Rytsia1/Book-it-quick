package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Bill {
    private Integer id;
    private Integer userId;
    private BigDecimal amount; // Uses BigDecimal for financial precision.
    private Integer type;      // 0 for expenses, 1 for income.
    private String category;
    private String description;
    private LocalDate billDate;
    private LocalDateTime createdAt;
}
