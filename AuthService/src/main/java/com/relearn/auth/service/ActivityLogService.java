package com.relearn.auth.service;

import com.relearn.auth.dto.ActivityLogResponse;
import com.relearn.auth.dto.PagedResponse;
import com.relearn.auth.entity.ActivityLog;
import com.relearn.auth.enums.ActivityType;
import com.relearn.auth.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for recording and retrieving activity log entries.
 *
 * Logging is done asynchronously (@Async) so it never blocks
 * the main request thread. If logging fails, the main operation
 * still succeeds — activity tracking is best-effort.
 */
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    // ================================================================
    //  WRITE — log events (called from other services)
    // ================================================================

    /**
     * Logs an activity event asynchronously.
     * This is the main entry point for all activity tracking.
     *
     * @param type        the type of event
     * @param description human-readable description
     * @param actorId     ID of the user who performed the action (null for system)
     * @param actorName   display name of the actor
     * @param actorRole   role of the actor
     * @param entityId    ID of the affected entity (optional)
     * @param entityType  type of the affected entity (optional)
     * @param metadata    extra context string (optional)
     */
    @Async
    @Transactional
    public void log(ActivityType type, String description,
                    Long actorId, String actorName, String actorRole,
                    Long entityId, String entityType, String metadata) {
        ActivityLog log = ActivityLog.builder()
                .activityType(type)
                .description(description)
                .actorId(actorId)
                .actorName(actorName)
                .actorRole(actorRole)
                .entityId(entityId)
                .entityType(entityType)
                .metadata(metadata)
                .build();
        activityLogRepository.save(log);
    }

    /**
     * Convenience overload — logs without entity or metadata context.
     */
    @Async
    @Transactional
    public void log(ActivityType type, String description,
                    Long actorId, String actorName, String actorRole) {
        log(type, description, actorId, actorName, actorRole, null, null, null);
    }

    // ================================================================
    //  READ — retrieve for admin dashboard
    // ================================================================

    /**
     * Returns the 20 most recent activity events.
     * Used for the admin dashboard activity feed.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRecentActivities() {
        return activityLogRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(ActivityLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns paginated activity log, optionally filtered by type.
     *
     * @param type     optional filter (null = all types)
     * @param page     page number (0-indexed)
     * @param size     page size
     */
    @Transactional(readOnly = true)
    public PagedResponse<ActivityLogResponse> getActivities(String type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ActivityLog> result;
        if (type != null && !type.isBlank()) {
            ActivityType activityType = ActivityType.valueOf(type.toUpperCase());
            result = activityLogRepository.findByActivityTypeOrderByCreatedAtDesc(
                    activityType, pageable);
        } else {
            result = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Page<ActivityLogResponse> mapped = result.map(ActivityLogResponse::fromEntity);
        return PagedResponse.from(mapped);
    }

    /**
     * Returns all activities for a specific actor (user).
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivitiesByActor(Long actorId) {
        return activityLogRepository.findByActorIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(ActivityLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns all activities for a specific entity.
     * e.g. all events related to note ID 5.
     */
    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivitiesByEntity(String entityType, Long entityId) {
        return activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(ActivityLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ================================================================
    //  STATS — used by dashboard activity percentage
    // ================================================================

    /**
     * Counts total events in the last N days.
     * Used to calculate system activity percentage.
     */
    public long countRecentEvents(int days) {
        return activityLogRepository.countByCreatedAtAfter(
                LocalDateTime.now().minusDays(days));
    }
}
