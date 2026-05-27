package com.relearn.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Relearn Auth Service.
 * This microservice handles authentication, user management, and activity logging.
 *
 * @EnableAsync enables asynchronous activity logging so it never blocks requests.
 */
@SpringBootApplication
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
