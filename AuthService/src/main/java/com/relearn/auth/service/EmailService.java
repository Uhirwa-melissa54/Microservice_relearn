package com.relearn.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional emails for the Relearn platform.
 *
 * All methods are @Async so email sending never blocks the main request thread.
 * If email fails, it is logged but does NOT cause the main operation to fail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    // ----------------------------------------------------------------
    //  Welcome email — sent when admin creates a new account
    // ----------------------------------------------------------------

    /**
     * Sends a welcome email with the system-generated password and assigned class.
     *
     * @param toEmail       recipient's email address
     * @param fullName      recipient's full name
     * @param role          STUDENT or TEACHER
     * @param rawPassword   the plain-text password (before BCrypt encoding)
     * @param className     the class assigned (e.g. "Y1A") — null for teachers
     * @param academicYear  academic year — null for teachers
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String role,
                                  String rawPassword, String className, String academicYear) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Relearn — Your Account Details");

            String classInfo = buildClassInfo(role, className, academicYear);

            String html = """
                    <div style="font-family: 'Poppins', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border-top: 6px solid #1A264A; box-shadow: 0 4px 20px rgba(0,0,0,0.08); padding: 40px;">
                      <h2 style="color: #1A264A; margin-bottom: 8px;">Welcome to Relearn! 🎓</h2>
                      <p style="color: #4b5563; font-size: 15px;">Hello <strong>%s</strong>,</p>
                      <p style="color: #4b5563; font-size: 15px;">
                        Your <strong>%s</strong> account on the Relearn platform has been created by your administrator.
                        Here are your login credentials:
                      </p>

                      <div style="background: #f3f4f6; border-radius: 8px; padding: 20px; margin: 24px 0;">
                        <p style="margin: 0 0 8px; color: #6b7280; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px;">LOGIN DETAILS</p>
                        <p style="margin: 6px 0; font-size: 15px;"><strong>Portal:</strong> <a href="%s" style="color: #1A264A;">%s</a></p>
                        <p style="margin: 6px 0; font-size: 15px;"><strong>Email:</strong> %s</p>
                        <p style="margin: 6px 0; font-size: 15px;"><strong>Password:</strong> <code style="background:#e5e7eb; padding: 2px 8px; border-radius: 4px; font-size: 14px;">%s</code></p>
                        %s
                      </div>

                      <div style="background: #fef3c7; border-radius: 8px; padding: 16px; margin-bottom: 24px; border-left: 4px solid #f59e0b;">
                        <p style="margin: 0; color: #92400e; font-size: 14px;">
                          ⚠️ <strong>Important:</strong> Please change your password immediately after your first login for security.
                        </p>
                      </div>

                      <a href="%s/login" style="display: inline-block; background-color: #1A264A; color: white; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 15px;">
                        Sign In Now →
                      </a>

                      <p style="color: #9ca3af; font-size: 12px; margin-top: 32px;">
                        This is an automated message from the Relearn platform. Please do not reply to this email.
                      </p>
                    </div>
                    """.formatted(
                    fullName, role.toLowerCase(),
                    baseUrl, baseUrl,
                    toEmail, rawPassword,
                    classInfo,
                    baseUrl
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    //  Reminder email — sent when user hasn't logged in after 1 day
    // ----------------------------------------------------------------

    /**
     * Sends a login reminder to a user who has never logged in.
     *
     * @param toEmail   recipient's email
     * @param fullName  recipient's name
     * @param daysSince number of days since account was created without login
     */
    @Async
    public void sendLoginReminder(String toEmail, String fullName, long daysSince) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reminder: Your Relearn Account is Waiting");

            String html = """
                    <div style="font-family: 'Poppins', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border-top: 6px solid #1A264A; box-shadow: 0 4px 20px rgba(0,0,0,0.08); padding: 40px;">
                      <h2 style="color: #1A264A; margin-bottom: 8px;">Don't forget to sign in! 👋</h2>
                      <p style="color: #4b5563; font-size: 15px;">Hello <strong>%s</strong>,</p>
                      <p style="color: #4b5563; font-size: 15px;">
                        Your Relearn account was created <strong>%d day(s) ago</strong> but you haven't signed in yet.
                        Your classes, notes, and assignments are ready and waiting for you.
                      </p>

                      <a href="%s/login" style="display: inline-block; background-color: #1A264A; color: white; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 15px; margin: 24px 0;">
                        Sign In Now →
                      </a>

                      <p style="color: #4b5563; font-size: 14px;">
                        If you have any issues signing in, please contact your administrator.
                      </p>
                      <p style="color: #9ca3af; font-size: 12px; margin-top: 32px;">
                        You will keep receiving reminders until you sign in for the first time.
                        This is an automated message from the Relearn platform.
                      </p>
                    </div>
                    """.formatted(fullName, daysSince, baseUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Login reminder sent to {} ({}d since creation)", toEmail, daysSince);

        } catch (MessagingException e) {
            log.error("Failed to send reminder to {}: {}", toEmail, e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    //  Helper
    // ----------------------------------------------------------------

    private String buildClassInfo(String role, String className, String academicYear) {
        if (!"STUDENT".equalsIgnoreCase(role)) return "";
        StringBuilder sb = new StringBuilder();
        if (className != null && !className.isBlank()) {
            sb.append("<p style='margin: 6px 0; font-size: 15px;'><strong>Class:</strong> ").append(className).append("</p>");
        }
        if (academicYear != null && !academicYear.isBlank()) {
            sb.append("<p style='margin: 6px 0; font-size: 15px;'><strong>Academic Year:</strong> ").append(academicYear).append("</p>");
        }
        return sb.toString();
    }
}
