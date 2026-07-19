package com.DTMK.Online.Bookkeeping.Website.Project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-request JWT filter. Runs as part of the Spring Security
 * filter chain (installed by {@code SecurityConfig#filterChain} via
 * {@code addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)}).
 * <p>
 * On every request:
 * <ol>
 *   <li>Read the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validate the JWT (HMAC signature + expiry) via
 *       {@code JwtUtil.validateToken(token)}.</li>
 *   <li>Extract {@code sub} (username) and {@code role} claims.</li>
 *   <li>Map the role to a Spring Security authority:
 *       {@code ROLE_USER} or {@code ROLE_ADMIN}. The
 *       {@code "ROLE_"} prefix is the Spring Security convention;
 *       {@code hasRole('ADMIN')} in the URL rules strips it before
 *       comparing, and {@code @PreAuthorize("hasRole('ADMIN')")}
 *       on controllers does the same.</li>
 *   <li>Populate the {@code SecurityContextHolder} with a
 *       {@link UsernamePasswordAuthenticationToken} carrying the
 *       username and authority list. We use {@code null} as the
 *       credentials (password) because the JWT is already validated;
 *       Spring Security only uses the authority list for the
 *       subsequent auth check.</li>
 * </ol>
 * <p>
 * If the header is missing or the token is invalid/expired, this
 * filter is a no-op. The {@code SecurityFilterChain} will then run
 * its authorization rules; if the endpoint is
 * {@code permitAll} (e.g. {@code /api/auth/login}), the request
 * continues. Otherwise the
 * {@code authenticationEntryPoint} fires and returns a 401.
 * <p>
 * <b>This filter does NOT consult the denylist table on every
 * request.</b> The denylist is still consulted only at
 * {@code /api/auth/refresh} and {@code /api/auth/logout} (where
 * an admin force-logout can take effect at the next refresh). On
 * regular endpoints, a denylisted access token is simply rejected
 * by signature validation once the token expires (max 15 min) or
 * the next refresh happens \u2014 a deliberate trade-off to keep
 * JWT's stateless performance.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            // No Bearer token — let the chain continue. The endpoint's
            // authorization rule (permitAll / hasRole / authenticated)
            // will decide what happens next.
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        String username = jwtUtil.getUsernameFromToken(token);
        if (username == null) {
            // Token failed validation (bad signature, expired, malformed).
            // Skip — the request will get a 401 from the entry point
            // when the chain reaches an authenticated() endpoint.
            chain.doFilter(req, res);
            return;
        }

        // Don't re-populate the SecurityContext if another filter
        // (e.g. a future OAuth2 filter) already authenticated this
        // request. The standard Spring Security idiom.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        // Pull the role claim and map to a Spring Security authority.
        // The role claim defaults to "USER" if missing (defensive).
        String role = jwtUtil.validateToken(token) == null
                ? "USER"
                : (String) jwtUtil.validateToken(token).get("role");
        if (role == null || role.isBlank()) role = "USER";

        // Spring Security convention: the authority name MUST start
        // with "ROLE_" for hasRole("X") to match. The @PreAuthorize
        // and hasRole methods strip the prefix when comparing.
        String authorityName = "ROLE_" + role;

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,                                // principal
                null,                                    // credentials (already validated)
                List.of(new SimpleGrantedAuthority(authorityName))   // authorities
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}
