package com.relearn.assignment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Lightweight HTTP client that calls the Auth Service to get student counts.
 *
 * Graceful degradation: if Auth Service is unavailable, returns 0
 * so the teacher dashboard still loads without crashing.
 */
@Slf4j
@Component
public class UserCountClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    /**
     * Returns the number of students enrolled in a specific class.
     * Calls GET /api/teacher/classes/{className}/students/count on the Auth Service.
     *
     * @param className e.g. "Y1A"
     * @param jwtToken  the JWT token to forward
     * @return student count, or 0 if the call fails
     */
    public long getStudentCountForClass(String className, String jwtToken) {
        try {
            String url = authServiceUrl + "/api/teacher/classes/" + className + "/students/count";

            HttpHeaders headers = new HttpHeaders();
            if (jwtToken != null && !jwtToken.isBlank()) {
                headers.set("Authorization", "Bearer " + jwtToken);
            }

            ResponseEntity<StudentCountDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), StudentCountDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getStudentCount();
            }
        } catch (Exception e) {
            log.warn("Could not fetch student count for class '{}': {}", className, e.getMessage());
        }
        return 0L;
    }

    /** Inner DTO matching Auth Service StudentCountResponse */
    public static class StudentCountDto {
        private String className;
        private long studentCount;
        public long getStudentCount() { return studentCount; }
        public void setStudentCount(long v) { this.studentCount = v; }
        public String getClassName() { return className; }
        public void setClassName(String v) { this.className = v; }
    }
}
