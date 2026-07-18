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
    /**
     * Soft-delete flag. 0 = live, 1 = moved to trash. Filtered out of
     * every read query in {@code CategoryMapper}, and the existing
     * {@code deleteCategory} endpoint is now backed by an
     * {@code UPDATE … SET is_deleted = 1} so the row is never actually
     * removed. A future "Trash" UI can restore by setting it back to 0.
     */
    private Integer isDeleted;
}
