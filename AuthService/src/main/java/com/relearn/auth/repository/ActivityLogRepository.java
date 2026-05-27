package com.relearn.auth.repository;

import com.relearn.auth.entity.ActivityLog;
import com.relearn.auth.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for ActivityLog entries.
 * All queries are read-only except save (logs are never updated).
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Most recent N activities — used for dashboard feed */
    List<ActivityLog> findTop20ByOrderByCreatedAtDesc();

    /** Paginated activity feed */
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Filter by activity type */
    Page<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(
            ActivityType activityType, Pageable pageable);

    /** Filter by actor (user who performed the action) */
    List<ActivityLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    /** Activities within a time range */
    List<ActivityLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    /** Count activities since a given time — used for activity percentage */
    long countByCreatedAtAfter(LocalDateTime since);

    /** Count activities by type since a given time */
    long countByActivityTypeAndCreatedAtAfter(ActivityType type, LocalDateTime since);

    /** Recent activities for a specific entity (e.g. all events for note ID 5) */
    List<ActivityLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, Long entityId);
}
