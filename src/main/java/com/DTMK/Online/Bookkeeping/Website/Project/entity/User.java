package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String avatar;
    private LocalDateTime createdAt;
    private BigDecimal monthlyBudget;
}
