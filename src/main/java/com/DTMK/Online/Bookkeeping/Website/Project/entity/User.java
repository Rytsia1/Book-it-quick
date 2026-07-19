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

    /**
     * RBAC role. One of {@code "USER"} (default for self-registered
     * accounts) or {@code "ADMIN"} (granted manually via a database
     * UPDATE or by an existing admin promoting the user via
     * {@code POST /api/admin/users/:id/role}). The Spring Security
     * layer in {@code SecurityConfig.java} uses this to decide
     * which endpoints the user can access via
     * {@code @PreAuthorize("hasRole('ADMIN')")}.
     * <p>
     * The role is also embedded in the JWT (claim {@code "role"}) so
     * the {@code JwtAuthenticationFilter} can populate the
     * {@code SecurityContext} with the right authority without a DB
     * lookup on every request.
     */
    private String role;       // "USER" or "ADMIN"
}
