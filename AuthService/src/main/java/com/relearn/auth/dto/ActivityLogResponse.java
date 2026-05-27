package com.relearn.auth.dto;

import com.relearn.auth.entity.ActivityLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning activity log entries.
 * Used in the admin dashboard recent activities feed.
 */
@Getter
@Setter
public class ActivityLogResponse {

    private Long id;
    private String activityType;
    private String description;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private Long entityId;
    private String entityType;
    private String metadata;
    private LocalDateTime createdAt;

    public static ActivityLogResponse fromEntity(ActivityLog log) {
        ActivityLogResponse r = new ActivityLogResponse();
        r.setId(log.getId());
        r.setActivityType(log.getActivityType().name());
        r.setDescription(log.getDescription());
        r.setActorId(log.getActorId());
        r.setActorName(log.getActorName());
        r.setActorRole(log.getActorRole());
        r.setEntityId(log.getEntityId());
        r.setEntityType(log.getEntityType());
        r.setMetadata(log.getMetadata());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }
}
