package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import com.DTMK.Online.Bookkeeping.Website.Project.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User request) {
        Map<String, Object> response = new HashMap<>();
        if (userMapper.findByUsername(request.getUsername()) != null) {
            response.put("error", "Username is already registered!");
            return ResponseEntity.badRequest().body(response);
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));
        request.setAvatar("default-avatar.png");
        userMapper.insertUser(request);

        response.put("message", "Registration successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Issue an access token (15 min) and a refresh token (7 days).
     * The frontend stores both in localStorage; the access token is
     * attached as {@code Authorization: Bearer ...} on every request,
     * and the refresh token is only sent to {@code /refresh} when the
     * access token expires.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User request) {
        Map<String, Object> response = new HashMap<>();
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("error", "Incorrect username or password!");
            return ResponseEntity.status(401).body(response);
        }

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
