package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.config.LoginRateLimitService;
import com.DTMK.Online.Bookkeeping.Website.Project.dto.LoginRequest;
import com.DTMK.Online.Bookkeeping.Website.Project.dto.RegisterRequest;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.service.AuthService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth") // Matches the frontend route configuration.
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginRateLimitService rateLimit;

    /**
     * Resolve the client's IP address. Honour {@code X-Forwarded-For}
     * if a reverse proxy (nginx, Cloudflare, load balancer) is in
     * front of Spring, otherwise fall back to {@code X-Real-IP} and
     * finally the raw socket address.
     */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {
        // @Valid on @RequestBody runs the Jakarta Bean Validation
        // annotations on RegisterRequest (NotBlank/Size/Pattern)
        // BEFORE this method body executes. If anything fails,
        // Spring throws MethodArgumentNotValidException which is
        // caught by GlobalExceptionHandler and returned as a
        // structured 400 response — so by the time we get here,
        // we know the username is sane and the password is non-empty.
        Map<String, Object> response = new HashMap<>();
        if (userMapper.findByUsername(request.getUsername()) != null) {
            response.put("error", "Username is already registered!");
            return ResponseEntity.badRequest().body(response);
        }

        // Map the validated DTO to the persistence entity.
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatar("default-avatar.png");
        userMapper.insertUser(user);

        response.put("message", "Registration successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Issue an access token (15 min) and a refresh token (7 days).
     * The frontend stores both in localStorage; the access token is
     * attached as {@code Authorization: Bearer ...} on every request,
     * and the refresh token is only sent to {@code /refresh} when the
     * access token expires.
     *
     * <h2>Brute-force protection</h2>
     * The {@link LoginRateLimitService} is consulted on every
     * FAILED login attempt. Each failed attempt consumes one
     * token from the per-IP bucket; after 5 failed attempts
     * within 15 minutes, the next attempt returns
     * {@code 429 Too Many Requests} with a {@code Retry-After}
     * header. Successful logins do NOT consume a token, so a
     * legitimate user who fat-fingers their password a few times
     * is not locked out as long as they eventually get it right.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        // @Valid runs the Jakarta Bean Validation annotations on
        // LoginRequest (NotBlank/Size/Pattern) BEFORE this method
        // body executes — so by the time we get here, the username
        // and password are guaranteed to be in the right shape.
        Map<String, Object> response = new HashMap<>();
        String ip = clientIp(httpRequest);

        // Look up the user FIRST so we can tell "unknown user" from
        // "wrong password" — both consume a token, but the order
        // doesn't matter for the rate limiter.
        User user = userMapper.findByUsername(request.getUsername());

        boolean ok = user != null
                && passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!ok) {
            // FAILED login: consume one token from this IP's bucket.
            // If the bucket is empty, return 429 immediately without
            // even revealing whether the credentials were right or
            // wrong (a small anti-enumeration bonus).
            ConsumptionProbe probe = rateLimit.tryConsume(ip);
            if (!probe.isConsumed()) {
                long waitSeconds = (long) Math.ceil(
                        probe.getNanosToWaitForRefill() / 1_000_000_000.0);
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Too many failed login attempts from this IP. "
                        + "Please try again in " + Math.max(1, waitSeconds / 60) + " minute(s).");
                err.put("retryAfterSeconds", waitSeconds);
                return ResponseEntity
                        .status(429)
                        .header("Retry-After", String.valueOf(waitSeconds))
                        .body(err);
            }
            response.put("error", "Incorrect username or password!");
            return ResponseEntity.status(401).body(response);
        }

        // SUCCESSFUL login: do NOT touch the bucket. Legitimate
        // users shouldn't be punished for their own typos.
        AuthService.TokenPair pair = authService.issueTokenPair(user);

        response.put("message", "Login successful");
        response.put("token", pair.accessToken());                          // access token (15 min)
        response.put("refreshToken", pair.refreshToken());                  // refresh token (7 days)
        response.put("accessTokenExpiresAt", pair.accessTokenExpiresAt().getTime());
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange a valid refresh token for a new (access, refresh) pair.
     * The old refresh token is rotated (marked revoked) so a stolen
     * token is single-use. The frontend calls this when its access
     * token expires (or proactively, ~1 min before expiry).
     * <p>
     * Request body: {@code { "refreshToken": "...", "accessToken": "..." (optional) }}.
     * <p>
     * Returns 401 on any failure (missing/revoked/expired token,
     * access token on denylist, or deleted user).
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body != null ? body.get("refreshToken") : null;
        String accessToken  = body != null ? body.get("accessToken")  : null;

        Optional<AuthService.TokenPair> pair = authService.refresh(refreshToken, accessToken);
        if (pair.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Invalid or expired refresh token");
            return ResponseEntity.status(401).body(err);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Token refreshed");
        response.put("token", pair.get().accessToken());
        response.put("refreshToken", pair.get().refreshToken());
        response.put("accessTokenExpiresAt", pair.get().accessTokenExpiresAt().getTime());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke the supplied access token (jti -> denylist) and refresh
     * token (hash -> revoked=1). Idempotent: calling with a
     * missing/already-revoked token is a successful no-op so the
     * frontend can safely retry on flaky networks.
     * <p>
     * Request body: {@code { "refreshToken": "...", "accessToken": "..." }}.
     * The access token can usually be read from the
     * {@code Authorization} header on the request, but the body
     * fallback is convenient for fetch() calls that strip headers
     * during preflight.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String accessToken  = body != null ? body.get("accessToken")  : null;
        String refreshToken = body != null ? body.get("refreshToken") : null;

        // Fall back to the Authorization header if the access token
        // wasn't included in the body. We strip the "Bearer " prefix
        // so JwtUtil receives just the raw token.
        if ((accessToken == null || accessToken.isBlank()) && authHeader != null
                && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring("Bearer ".length()).trim();
        }

        authService.logout(accessToken, refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out");
        return ResponseEntity.ok(response);
    }
}
