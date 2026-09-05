package io.commercedna.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CommerceDNA Main Application Entrypoint.
 * AI-Native Merchant Identity & Transaction Protocol for Agentic Commerce.
 */
@SpringBootApplication(scanBasePackages = "io.commercedna")
@EnableJpaRepositories(basePackages = "io.commercedna")
@EntityScan(basePackages = "io.commercedna")
@EnableScheduling
public class CommerceDnaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceDnaApplication.class, args);
    }
}
