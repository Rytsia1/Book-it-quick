package com.DTMK.Online.Bookkeeping.Website.Project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Applies to all endpoints (/api/user, /api/bill, etc.)
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // Allows Vue/Vite frontend URLs.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allows these HTTP methods.
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
