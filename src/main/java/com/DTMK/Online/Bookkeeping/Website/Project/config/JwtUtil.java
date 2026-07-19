package com.DTMK.Online.Bookkeeping.Website.Project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT helper + opaque-refresh-token generator.
 *
 * <h2>Access token (JWT, 15-minute lifetime)</h2>
 * Short-lived, stateless, signed with HS256. Every token carries a
 * unique {@code jti} (UUID v4) so the denylist table can target a
 * specific token if the server ever needs to revoke it before its
 * natural expiry (e.g. an admin force-logout). The denylist is only
 * consulted at {@code /api/auth/refresh} and {@code /api/auth/logout}
 * &mdash; never on every request &mdash; to preserve the stateless
 * performance of JWT.
 *
 * <h2>Refresh token (opaque random string, 7-day lifetime)</h2>
 * A 256-bit cryptographically random value, base64url-encoded. It is
 * <i>not</i> a JWT so the server has full authority to revoke it
 * (just flip {@code revoked = 1} in {@code t_refresh_token}). We only
 * store the SHA-256 hash, never the raw token, so a database leak
 * does not directly leak working refresh tokens.
 */
@Component
public class JwtUtil {

    /** Access token lifetime: 15 minutes (per spec). */
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L;

    /** Refresh token lifetime: 7 days. Long enough to be useful, short enough to limit blast radius. */
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    private static final SecureRandom RNG = new SecureRandom();

    // Secret key used to sign tokens (minimum 256-bit).
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // ──────────────────────────── Access token (JWT) ────────────────────────────

    /**
     * Issue a fresh access token. Each call gets a unique {@code jti}
     * (UUID) so the denylist can target individual tokens.
     */
    public String generateAccessToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())         // jti claim
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Parse and cryptographically validate a JWT. Returns the parsed
     * claims on success, or {@code null} on any failure (expired,
     * malformed, bad signature, unsupported). The caller should treat
     * a {@code null} return as "401 Unauthorized" without leaking the
     * specific reason to the client.
     */
    public Claims validateToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | MalformedJwtException
                 | UnsupportedJwtException | SignatureException | IllegalArgumentException e) {
            return null;
        } catch (JwtException e) {
            // Catch-all for any other jjwt-thrown subclass so a future
            // library bump can't crash the request thread.
            return null;
        }
    }

    /** {@code sub} claim, or {@code null} if the token can't be parsed. */
    public String getUsernameFromToken(String token) {
        Claims c = validateToken(token);
        return c != null ? c.getSubject() : null;
    }

    /** {@code jti} claim, or {@code null} if the token can't be parsed. */
    public String getJtiFromToken(String token) {
        Claims c = validateToken(token);
        return c != null ? c.getId() : null;
    }

    /** Expiration instant, or {@code null} if the token can't be parsed. */
    public Date getExpirationFromToken(String token) {
        Claims c = validateToken(token);
        return c != null ? c.getExpiration() : null;
    }

    // ──────────────────────────── Refresh token (opaque) ────────────────────────────

    /**
     * Generate a new opaque refresh token: 256 random bits encoded as
     * base64url (no padding), so the wire form is URL-safe and ~43
     * characters long. The caller is responsible for persisting
     * {@link #hash(String)} (not the raw value) to the database.
     */
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 hex digest of the refresh token, used as the lookup key
     * in {@code t_refresh_token.token_hash}. SHA-256 is the right
     * choice here: refresh tokens are high-entropy random strings, so
     * a slow KDF like bcrypt would be overkill and would hurt DB
     * performance without adding any real security.
     */
    public String hash(String refreshToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandatory in every JDK; this branch is unreachable.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
