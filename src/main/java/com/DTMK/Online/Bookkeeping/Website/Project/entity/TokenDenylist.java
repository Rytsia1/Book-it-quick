package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A revoked access-token entry. Looked up by {@code jti} (the JWT ID
 * claim) at {@code /api/auth/refresh} and {@code /api/auth/logout}
 * <i>only</i> — not on every request, so the database cost stays flat
 * regardless of traffic. Rows whose {@code expiresAt} is in the past are
 * pruned by {@code TokenCleanupScheduler} so the table never grows
 * unbounded.
 * <p>
 * See {@code t_token_denylist} in {@code schema.sql} for column rationale.
 */
@Data
public class TokenDenylist {
    private Integer id;
    private String jti;
    private Integer userId;
    private LocalDateTime expiresAt;
    private String reason;
    private LocalDateTime createdAt;
}
