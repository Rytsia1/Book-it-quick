package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryStatDTO {
    private String name;  // Category name (for example: "Food")
    private BigDecimal value; // Total expenses (for example: 50000.00)
}
