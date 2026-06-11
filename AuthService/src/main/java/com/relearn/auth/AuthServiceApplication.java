package com.relearn.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Relearn Auth Service.
 *
 * @EnableAsync      — activity logging + email sending never block requests
 * @EnableScheduling — login reminder scheduler runs every 6 hours
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
