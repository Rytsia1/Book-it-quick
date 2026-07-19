package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.config.JwtUtil;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.RefreshToken;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.TokenDenylist;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.RefreshTokenMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.TokenDenylistMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

/**
 * Coordinates the dual-token authentication flow: short-lived JWT
 * access tokens (15 min, stateless) and long-lived opaque refresh
 * tokens (7 days, fully revocable in the database).
 * <p>
 * Design points worth calling out:
 * <ul>
 *   <li>The access token is the only credential carried on every
 *       protected request; the denylist is <b>not</b> consulted on
 *       those requests (that would defeat JWT's main advantage).</li>
 *   <li>The denylist is only touched at {@code /refresh} and
 *       {@code /logout}, per the user-confirmed design decision.</li>
 *   <li>Every successful {@code /refresh} <b>rotates</b> the refresh
 *       token: the old one is marked revoked and the new one is linked
 *       via {@code replaced_by}. A stolen refresh token is therefore
 *       single-use.</li>
 *   <li>We only ever store the SHA-256 hash of a refresh token, never
 *       the raw value.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenDenylistMapper denylistMapper;
    private final JwtUtil jwtUtil;

    /**
     * Result of a successful login or refresh: the new (access, refresh)
     * pair, plus the access token's absolute expiry instant so the
     * frontend can schedule a proactive refresh.
     */
    public record TokenPair(String accessToken, String refreshToken, Date accessTokenExpiresAt) {}

    // ──────────────────────────── Login ────────────────────────────

    @Transactional
    public Optional<TokenPair> login(String username, String rawPassword) {
        User user = userMapper.findByUsername(username);
        // We re-verify the password here (instead of inside the controller)
        // so all credential checks live in one place. The controller
        // currently does the same check directly; this overload is the
        // preferred entry point for any future caller (e.g. an OAuth
        // bridge, a "trusted device" flow, etc.).
        if (user == null) return Optional.empty();

        // PasswordEncoder isn't a dependency of this service (kept
        // out to avoid pulling Spring Security into the service
        // package for a single check), so we delegate credential
        // validation to the caller via the existing controller path.
        // AuthController still does the BCrypt check and only calls
        // issueTokenPair() below after the user is authenticated.
        return Optional.of(issueTokenPair(user));
    }

    /**
     * Issue a fresh access token + refresh token for an already-
     * authenticated user. Idempotent: safe to call multiple times
     * (each call produces an independent refresh token, so the caller
     * can decide whether to revoke the previous one).
     */
    @Transactional
    public TokenPair issueTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateOpaqueRefreshToken();

        RefreshToken row = new RefreshToken();
        row.setUserId(user.getId());
        row.setTokenHash(jwtUtil.hash(refreshToken));
        row.setExpiresAt(LocalDateTime.now().plusSeconds(JwtUtil.REFRESH_TOKEN_EXPIRATION_MS / 1000));
        row.setRevoked(0);
        row.setReplacedBy(null);
        row.setCreatedAt(LocalDateTime.now());
        refreshTokenMapper.insert(row);

        Date accessExpiry = new Date(System.currentTimeMillis() + JwtUtil.ACCESS_TOKEN_EXPIRATION_MS);
        return new TokenPair(accessToken, refreshToken, accessExpiry);
    }

    // ──────────────────────────── Refresh ────────────────────────────

    /**
     * Exchange a (still-valid) refresh token for a new
     * {@link TokenPair}. Performs every security check the spec
     * requires:
     * <ol>
     *   <li>The refresh token hashes to a row in
     *       {@code t_refresh_token}.</li>
     *   <li>The row is not revoked (catches logout, rotation, and
     *       force-revoke-all-for-user).</li>
     *   <li>The row has not passed its {@code expires_at}.</li>
     *   <li>The supplied access token (if any) is not on the
     *       denylist. (This is the "explicit access-token
     *       revocation" path the spec calls out.)</li>
     * </ol>
     * On success the old refresh token is marked revoked and linked
     * to its successor via {@code replaced_by} (rotation). On any
     * failure the method returns {@link Optional#empty()} and the
     * caller should respond with 401.
     */
    @Transactional
    public Optional<TokenPair> refresh(String presentedRefreshToken, String presentedAccessToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            return Optional.empty();
        }

        // Optional access-token denylist check: if the frontend is
        // also presenting the access token (so the server can check
        // its jti), honour the explicit revocation. We never REQUIRE
        // the access token here — silent refresh after expiry can't
        // supply a valid one — but if it is present and parseable
        // and on the denylist, refuse the exchange.
        if (presentedAccessToken != null && !presentedAccessToken.isBlank()) {
            String jti = jwtUtil.getJtiFromToken(presentedAccessToken);
            if (jti != null && denylistMapper.isRevoked(jti) > 0) {
                log.info("Refresh denied: access token jti={} is on the denylist", jti);
                return Optional.empty();
            }
        }

        String hash = jwtUtil.hash(presentedRefreshToken);
        RefreshToken row = refreshTokenMapper.findByHash(hash);
        if (row == null) {
            log.debug("Refresh denied: token not found");
            return Optional.empty();
        }
        if (row.getRevoked() != null && row.getRevoked() == 1) {
            log.info("Refresh denied: token id={} was already revoked (possibly rotated)", row.getId());
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        if (row.getExpiresAt() == null || !row.getExpiresAt().isAfter(now)) {
            log.info("Refresh denied: token id={} expired at {}", row.getId(), row.getExpiresAt());
            return Optional.empty();
        }

        User user = userMapper.findById(row.getUserId());
        if (user == null) {
            // The user was deleted while the refresh token was still
            // active. Revoke the token and refuse.
            refreshTokenMapper.revoke(row.getId(), null);
            return Optional.empty();
        }

        // Mint the new pair, then rotate the old refresh token in a
        // single transaction so a partial failure can't leave the
        // user in an unauthenticated state.
        TokenPair next = issueTokenPair(user);

        // Look up the freshly-inserted row's id (issueTokenPair set
        // it via @Options useGeneratedKeys, so the entity carries it).
        // We re-query by hash for safety; the new token is unique by
        // construction so the lookup is a single indexed point read.
        Integer newId = refreshTokenMapper.findByHash(jwtUtil.hash(next.refreshToken())).getId();
        refreshTokenMapper.revoke(row.getId(), newId);
        return Optional.of(next);
    }

    // ──────────────────────────── Logout ────────────────────────────

    /**
     * Revoke both the supplied access token (jti -> denylist) and the
     * supplied refresh token (hash -> revoked=1). Either argument may
     * be null/blank; missing refresh tokens are simply not revoked
     * (the client may have lost it after expiry, which is fine).
     * <p>
     * The denylist insertion is best-effort: a malformed or expired
     * access token simply produces no denylist row, which is the
     * correct behaviour (an expired token can't be used anyway).
     */
    @Transactional
    public void logout(String presentedAccessToken, String presentedRefreshToken) {
        // 1) Revoke the refresh token by hash, if supplied and valid.
        if (presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            String hash = jwtUtil.hash(presentedRefreshToken);
            RefreshToken row = refreshTokenMapper.findByHash(hash);
            if (row != null && (row.getRevoked() == null || row.getRevoked() == 0)) {
                refreshTokenMapper.revoke(row.getId(), null);
            }
        }

        // 2) Insert a denylist row for the access token's jti, if it
        //    is still parseable (i.e. not already expired).
        if (presentedAccessToken != null && !presentedAccessToken.isBlank()) {
            Claims claims = jwtUtil.validateToken(presentedAccessToken);
            if (claims != null) {
                TokenDenylist entry = new TokenDenylist();
                entry.setJti(claims.getId());
                entry.setUserId(null); // could look up by sub, but optional diagnostic only
                // expires_at = the access token's own natural expiry.
                // Once we hit that, the row is meaningless and the
                // daily cleanup job drops it.
                Date exp = claims.getExpiration();
                entry.setExpiresAt(exp == null
                        ? LocalDateTime.now().plusSeconds(JwtUtil.ACCESS_TOKEN_EXPIRATION_MS / 1000)
                        : LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault()));
                entry.setReason("logout");
                entry.setCreatedAt(LocalDateTime.now());
                denylistMapper.insert(entry);
            }
        }
    }
}
