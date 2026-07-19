package com.DTMK.Online.Bookkeeping.Website.Project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 *
 * <h2>What this class does</h2>
 * <ul>
 *   <li>Wires a real {@link SecurityFilterChain} (the previous version
 *       used {@code anyRequest().permitAll()}, which meant there was
 *       <b>no</b> authentication at all).</li>
 *   <li>Installs a {@link JwtAuthenticationFilter} that reads the
 *       {@code Authorization: Bearer ...} header on every request,
 *       validates the JWT, and populates the Spring SecurityContext
 *       with the user's role as a {@code ROLE_USER} or
 *       {@code ROLE_ADMIN} authority. After this, the standard
 *       {@code @PreAuthorize("hasRole('ADMIN')")} annotation works
 *       out of the box.</li>
 *   <li>Enables {@code @EnableMethodSecurity(prePostEnabled = true)}
 *       so controllers and service methods can use
 *       {@code @PreAuthorize}, {@code @PostAuthorize}, etc.</li>
 *   <li>Configures per-endpoint authorization rules:
 *       {@code /api/auth/**} (login/register/refresh) is
 *       {@code permitAll}; {@code /api/admin/**} requires
 *       {@code hasRole('ADMIN')}; everything else is
 *       {@code authenticated()} (a JWT must be present).</li>
 *   <li>Configures custom JSON {@code authenticationEntryPoint} and
 *       {@code accessDeniedHandler} so 401/403 responses use the
 *       same JSON envelope the rest of the API uses (handled by
 *       {@link GlobalExceptionHandler}).</li>
 * </ul>
 *
 * <h2>Why we don't use Spring Security's UserDetailsService</h2>
 * Because we're JWT-only. The JWT is self-validating (HMAC signature)
 * and carries the role in its claims, so we don't need a DB lookup
 * on every request. The {@code JwtAuthenticationFilter} just trusts
 * the token's signature + expiry + denylist check and maps the
 * role claim to a Spring Security authority.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // enables @PreAuthorize
public class SecurityConfig {

    /**
     * BCrypt password encoder. Same strength as the previous version;
     * kept here so {@code AuthController} can keep using
     * {@code PasswordEncoder} for registration.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager bean. Required by Spring Security even
     * though we don't use form login — the JWT filter needs it to
     * be available in the application context.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /**
     * The actual security filter chain.
     * <p>
     * Endpoint rules:
     * <ul>
     *   <li>{@code /api/auth/**}    \u2014 permitAll (login/register/refresh
     *       must be reachable without a token, otherwise the user
     *       can never log in to begin with).</li>
     *   <li>{@code /api/admin/**}   \u2014 hasRole('ADMIN') (the
     *       {@code @PreAuthorize("hasRole('ADMIN')")} annotation
     *       on the controller is a second layer of defence).</li>
     *   <li>everything else         \u2014 authenticated (a valid JWT
     *       must be present in the Authorization header).</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // No HTTP-basic or form-login: auth is JWT-only.
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            // Insert the JWT filter BEFORE the standard
            // UsernamePasswordAuthenticationFilter so the SecurityContext
            // is populated before any auth check runs.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Custom JSON 401 / 403 responses. The frontend's
            // request.js interceptor reads `response.data.message` to
            // show the toast; this matches the GlobalExceptionHandler
            // envelope shape ({success, message, status, path, timestamp}).
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                        "{\"success\":false,\"message\":\"Authentication required\","
                      + "\"status\":401,\"path\":\"" + req.getRequestURI() + "\","
                      + "\"timestamp\":\"" + java.time.Instant.now() + "\"}");
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                        "{\"success\":false,\"message\":\"Access denied: ADMIN role required\","
                      + "\"status\":403,\"path\":\"" + req.getRequestURI() + "\","
                      + "\"timestamp\":\"" + java.time.Instant.now() + "\"}");
                })
            );
        return http.build();
    }

    /**
     * CORS configuration. Same shape as before so the Vue dev
     * server (http://localhost:5173 / :3000) can keep talking to
     * Spring. Allowed headers now also include {@code Authorization}
     * so the JWT Bearer token can be sent cross-origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
