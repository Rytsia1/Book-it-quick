package com.DTMK.Online.Bookkeeping.Website.Project.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistent record of a refresh token issued to a user.
 * <p>
 * The actual token is a random opaque string sent to the client; only
 * its SHA-256 hash is stored so a database leak does not directly leak
 * working refresh tokens. See {@code t_refresh_token} in
 * {@code schema.sql} for the column rationale.
 */
@Data
public class RefreshToken {
    private Integer id;
    private Integer userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    /** 0 = active, 1 = revoked. Mapped to MySQL TINYINT(1). */
    private Integer revoked;
    /** FK -> t_refresh_token.id of the row that replaced this one. */
    private Integer replacedBy;
    private LocalDateTime createdAt;
}
