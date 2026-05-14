package com.relearn.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Relearn Notes Service.
 *
 * This microservice is solely responsible for managing notes
 * (create, read, update, delete) for the Relearn school platform.
 *
 * It does NOT handle authentication, assignments, or any other domain.
 */
@SpringBootApplication
public class NotesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotesServiceApplication.class, args);
    }
}
