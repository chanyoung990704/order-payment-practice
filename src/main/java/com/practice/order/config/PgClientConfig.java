package com.practice.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("real")
public class PgClientConfig {

    @Bean
    RestTemplate paymentRestTemplate() {
        return new RestTemplate();
    }
}
