package vn.techshop.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class EmailOutboxService {
    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);

    public record OutboxItem(
        Long id,
        String recipient,
        String subject,
        String body,
        int retryCount,
        int maxRetries
    ) {}

    private final JdbcTemplate jdbc;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailOutboxService(
        JdbcTemplate jdbc,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${app.mail.from:no-reply@techzone.local}") String fromAddress
    ) {
        this.jdbc = jdbc;
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress != null && !fromAddress.isBlank() ? fromAddress : "no-reply@techzone.local";
    }

    @Transactional
    public Long enqueue(String recipient, String subject, String body) {
        Instant now = Instant.now();
        jdbc.update(
            "insert into email_outbox (recipient, subject, body, status, retry_count, max_retries, next_retry_at, created_at) " +
            "values (?, ?, ?, 'PENDING', 0, 3, ?, ?)",
            recipient, subject, body, Timestamp.from(now), Timestamp.from(now)
        );
        Long id = jdbc.queryForObject("select max(id) from email_outbox where recipient = ?", Long.class, recipient);
        return id;
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox-poll-ms:10000}")
    public int processPendingEmails() {
        var sender = mailSenderProvider.getIfAvailable();
        Instant now = Instant.now();
        List<OutboxItem> pending = jdbc.query(
            "select id, recipient, subject, body, retry_count, max_retries from email_outbox " +
            "where status = 'PENDING' and next_retry_at <= ? and retry_count < max_retries " +
            "order by id asc limit 20",
            (rs, rowNum) -> new OutboxItem(
                rs.getLong("id"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getInt("retry_count"),
                rs.getInt("max_retries")
            ),
            Timestamp.from(now)
        );

        int processed = 0;
        for (var item : pending) {
            if (sender == null) {
                // Chưa có cấu hình mail sender thì giữ PENDING để retry khi có cấu hình
                continue;
            }
            try {
                var message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(item.recipient());
                message.setSubject(item.subject());
                message.setText(item.body());
                sender.send(message);

                jdbc.update(
                    "update email_outbox set status = 'SENT', sent_at = ?, error_message = null where id = ?",
                    Timestamp.from(Instant.now()), item.id()
                );
                processed++;
            } catch (Exception ex) {
                int nextRetryCount = item.retryCount() + 1;
                boolean reachedMax = nextRetryCount >= item.maxRetries();
                String errSummary = ex.getClass().getSimpleName() + (ex.getMessage() != null ? ": " + ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 200)) : "");
                
                if (reachedMax) {
                    jdbc.update(
                        "update email_outbox set status = 'FAILED', retry_count = ?, error_message = ? where id = ?",
                        nextRetryCount, errSummary, item.id()
                    );
                    log.warn("Email outbox item {} failed permanently after {} retries: {}", item.id(), nextRetryCount, ex.getClass().getSimpleName());
                } else {
                    // Exponential backoff: 30s, 60s, 120s
                    long delaySeconds = (long) Math.pow(2, nextRetryCount) * 15L;
                    Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);
                    jdbc.update(
                        "update email_outbox set retry_count = ?, next_retry_at = ?, error_message = ? where id = ?",
                        nextRetryCount, Timestamp.from(nextRetryAt), errSummary, item.id()
                    );
                    log.warn("Email outbox item {} failed attempt {}, will retry at {}: {}", item.id(), nextRetryCount, nextRetryAt, ex.getClass().getSimpleName());
                }
            }
        }
        return processed;
    }

    public List<OutboxItem> getPending() {
        return jdbc.query(
            "select id, recipient, subject, body, retry_count, max_retries from email_outbox where status = 'PENDING' order by id asc",
            (rs, rowNum) -> new OutboxItem(
                rs.getLong("id"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getInt("retry_count"),
                rs.getInt("max_retries")
            )
        );
    }
}
