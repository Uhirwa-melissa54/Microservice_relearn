package com.relearn.auth.service;

import com.relearn.auth.entity.User;
import com.relearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that reminds users who have never logged in.
 *
 * Logic:
 *  - Runs every 6 hours (configurable via app.reminder.cron)
 *  - Finds all active users where lastLoginAt IS NULL (never logged in)
 *    AND account was created more than 1 day ago
 *  - Sends a reminder email to each
 *
 * The reminder stops automatically when the user logs in
 * because login sets lastLoginAt, which excludes them from the query.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginReminderScheduler {

    private final UserRepository userRepository;
    private final EmailService   emailService;

    @Scheduled(cron = "${app.reminder.cron:0 0 */6 * * *}")
    public void sendLoginReminders() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        List<User> neverLoggedIn = userRepository
                .findByActiveAndLastLoginAtIsNullAndCreatedAtBefore(true, oneDayAgo);

        if (neverLoggedIn.isEmpty()) {
            log.debug("Reminder scheduler: no users need reminding.");
            return;
        }

        log.info("Reminder scheduler: sending reminders to {} user(s)", neverLoggedIn.size());

        for (User user : neverLoggedIn) {
            long daysSince = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
            emailService.sendLoginReminder(user.getEmail(), user.getFullName(), daysSince);
        }
    }
}
