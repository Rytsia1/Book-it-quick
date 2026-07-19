package com.DTMK.Online.Bookkeeping.Website.Project.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Exposes a single, application-wide {@link RestTemplate} bean with
 * conservative timeouts so the currency-sync job (and any future
 * outbound HTTP call) cannot hang the scheduler thread indefinitely
 * if the upstream API is slow or unreachable.
 * <p>
 * Timeouts:
 * <ul>
 *   <li>connect: 5 seconds</li>
 *   <li>read:    10 seconds (the exchangerate-api.com /latest/USD
 *       endpoint normally responds in <1 s; 10 s is a generous
 *       upper bound)</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
