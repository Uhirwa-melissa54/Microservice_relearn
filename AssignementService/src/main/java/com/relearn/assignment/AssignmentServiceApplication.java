package com.relearn.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Relearn Assignment Service.
 *
 * This microservice is solely responsible for managing assignments
 * and student submissions on the Relearn school platform.
 *
 * It does NOT handle authentication, notes, or any other domain.
 */
@SpringBootApplication
public class AssignmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssignmentServiceApplication.class, args);
    }
}
