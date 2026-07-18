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
    /**
     * Soft-delete flag. 0 = live, 1 = moved to trash. Filtered out of
     * every read query in {@code BillMapper}, and the existing
     * {@code deleteBill} endpoint is now backed by an
     * {@code UPDATE … SET is_deleted = 1} so the row is never actually
     * removed — only hidden. See {@code schema.sql} for the column DDL
     * and the industry-standard rationale.
     */
    private Integer isDeleted;
}
