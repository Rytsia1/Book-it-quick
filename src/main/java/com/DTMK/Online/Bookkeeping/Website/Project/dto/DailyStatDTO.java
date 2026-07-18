package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyStatDTO {
    private LocalDate date;     // Date (for example: 2026-07-16)
    private BigDecimal income;  // Total income on that date
    private BigDecimal expense; // Total expenses on that date
}
