package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Category {
    private Integer id;
    private Integer userId;
    private Integer type; // 0 = expense, 1 = income
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
