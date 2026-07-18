package com.DTMK.Online.Bookkeeping.Website.Project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Mendaftarkan "Alat" Enkripsi BCrypt ke Spring Boot
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS configuration source used by the Spring Security filter chain.
     * <p>
     * <b>Why this is here, separately from {@code CorsConfig}:</b>
     * When {@code spring-boot-starter-security} is on the classpath, every
     * request (including the CORS preflight {@code OPTIONS} request that
     * the browser sends before any non-simple {@code POST}/{@code PUT}/
     * {@code DELETE} with {@code Content-Type: application/json}) is
     * handled by the {@code SecurityFilterChain} <i>before</i> it reaches
     * Spring MVC's {@code WebMvcConfigurer} CORS handler.
     * <p>
     * If the security chain does not have its own CORS configuration, the
     * preflight is rejected with 403, the browser cancels the real request,
     * and the frontend sees a silent "Network Error" / "Failed to save"
     * even though the controller would have handled the request fine.
     * <p>
     * Exposing this bean and calling {@code http.cors(Customizer.withDefaults())}
     * below makes the security filter chain use these rules for preflight
     * handling, which is what makes POST/PUT/DELETE actually reach the
     * {@code BillController}, {@code CategoryController}, etc.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Must mirror the origins listed in CorsConfig.java.
        cfg.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow every request header (Authorization, Content-Type, etc.).
        cfg.setAllowedHeaders(List.of("*"));
        // Expose the headers the frontend may need to read from the response.
        cfg.setExposedHeaders(List.of("Authorization"));
        // Required so the browser sends the Authorization header / cookies.
        cfg.setAllowCredentials(true);
        // Cache the preflight result for 1 hour to avoid extra round-trips.
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // 2. Mematikan blokir bawaan agar Frontend bisa mengakses API kita
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS in the security chain so OPTIONS preflight
                // requests from the browser succeed. Without this, the
                // WebMvcConfigurer CORS rules in CorsConfig.java are never
                // reached for preflight and POST/PUT/DELETE silently fail.
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Izinkan semua endpoint (login/register/bills) bisa diakses
                );
        return http.build();
    }
}
