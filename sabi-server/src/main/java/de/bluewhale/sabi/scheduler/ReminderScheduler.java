/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.scheduler;

import de.bluewhale.sabi.persistence.model.ReminderDeliveryRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.ReminderDeliveryRecordRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.services.MeasurementReminderEmail;
import de.bluewhale.sabi.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Daily scheduler for email measurement reminders (sabi-69).
 *
 * <p>Runs off-request at the configured cron time (FR-003). Selects all due users in one
 * read query, then processes each user in its <b>own transaction</b> (R8): a failure for
 * one user rolls back only that user's state and never blocks the other recipients.</p>
 *
 * <p>Per-user/cycle state machine (data-model.md §2):</p>
 * <ul>
 *   <li>existing record with status SENT → skip (idempotency, SC-003)</li>
 *   <li>empty content from all providers → no record, advance next send date (R6)</li>
 *   <li>send OK → record SENT + sentAt, advance next send date</li>
 *   <li>transient {@link MailSendException} → attempts += 1, status FAILED, no advance;
 *       retried on the next run</li>
 *   <li>attempts &ge; 3 or permanent rejection (bounce) → cycle closed, advance next send date</li>
 * </ul>
 *
 * <p>Logging follows the PII rule: {@code user_id=} only, never email addresses.</p>
 *
 * @author Stefan Schubert
 */
@Component
@Slf4j
public class ReminderScheduler {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final int MAX_ATTEMPTS = 3;

    private final Clock clock;
    private final UserRepository userRepository;
    private final ReminderDeliveryRecordRepository deliveryRecordRepository;
    private final NotificationService notificationService;
    private final List<ReminderContentProvider> contentProviders;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ReminderScheduler(Clock clock,
                             UserRepository userRepository,
                             ReminderDeliveryRecordRepository deliveryRecordRepository,
                             NotificationService notificationService,
                             List<ReminderContentProvider> contentProviders,
                             PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.userRepository = userRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.notificationService = notificationService;
        this.contentProviders = contentProviders;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Executes one scheduler run: evaluates all due users and sends at most one email
     * per user and cycle (SC-002/SC-003).
     */
    @Scheduled(cron = "${sabi.reminder.scheduler-cron}")
    public void run() {
        LocalDate runDate = LocalDate.now(clock);
        List<UserEntity> dueUsers =
                userRepository.findByEmailReminderEnabledTrueAndEmailReminderNextSendDateLessThanEqual(runDate);
        log.info("Reminder scheduler run started, due users={}", dueUsers.size());

        for (UserEntity user : dueUsers) {
            // Only the ID is used: the selection query runs outside a transaction
            // (Spring Data JPA 4 read methods are not @Transactional), so the returned
            // entities are detached with uninitialized lazy collections. The per-user
            // transaction re-fetches the user (see processUserInTransaction).
            processUser(user.getId());
        }

        log.info("Reminder scheduler run finished");
    }

    /**
     * Processes one due user in its own transaction (R8). Any exception is contained:
     * it rolls back only this user's state and the batch continues with the next user.
     */
    private void processUser(Long userId) {
        try {
            transactionTemplate.executeWithoutResult(status -> processUserInTransaction(userId));
        } catch (Exception e) {
            log.error("Failed to process reminder cycle for user_id={}: {}", userId, e.getMessage());
        }
    }

    private void processUserInTransaction(Long userId) {
        // Re-fetch inside this transaction: the entity is managed here, lazy collections
        // load on access, and the next-send-date change below is flushed by dirty checking.
        UserEntity user = userRepository.findById(userId).orElseThrow();
        LocalDate cycleDate = user.getEmailReminderNextSendDate();
        if (cycleDate == null) {
            // disabled in the meantime or data anomaly → nothing to do
            return;
        }
        int intervalDays = user.getEmailReminderIntervalDays() != null
                ? user.getEmailReminderIntervalDays() : 7;

        // Idempotency (SC-003): a SENT record for this cycle means the email already went out
        Optional<ReminderDeliveryRecordEntity> existing =
                deliveryRecordRepository.findByUserIdAndCycleDate(user.getId(), cycleDate);
        if (existing.isPresent() && STATUS_SENT.equals(existing.get().getStatus())) {
            log.debug("Reminder cycle already sent for user_id={} cycle={}", user.getId(), cycleDate);
            return;
        }

        // Build content from all registered providers (SPI, FR-011)
        List<MeasurementReminderEmail> payloads = contentProviders.stream()
                .map(provider -> provider.buildContent(user, cycleDate))
                .flatMap(Optional::stream)
                .toList();

        if (payloads.isEmpty()) {
            // R6: nothing overdue → no record, but the cycle still closes and advances.
            // A stale FAILED record (if any) is left in place as audit evidence.
            user.setEmailReminderNextSendDate(cycleDate.plusDays(intervalDays));
            log.info("No overdue content for user_id={} cycle={}, advancing next send date", user.getId(), cycleDate);
            return;
        }

        ReminderDeliveryRecordEntity record = existing.orElseGet(() -> {
            ReminderDeliveryRecordEntity fresh = new ReminderDeliveryRecordEntity();
            fresh.setUser(user);
            // Register in the parent collection: user.reminderDeliveryRecords is mapped with
            // orphanRemoval=true, so a record inserted via the child repository alone would be
            // deleted as an orphan at commit (see AGENTS.md JPA pattern).
            user.getReminderDeliveryRecords().add(fresh);
            fresh.setCycleDate(cycleDate);
            fresh.setStatus(STATUS_FAILED);
            fresh.setAttempts(0);
            return deliveryRecordRepository.saveAndFlush(fresh);
        });

        int failedSends = 0;
        boolean permanentRejection = false;
        for (MeasurementReminderEmail payload : payloads) {
            try {
                notificationService.sendMeasurementReminderEmail(payload, localeOf(user));
            } catch (MailSendException e) {
                // transient SMTP failure → retry on the next scheduler run (R7)
                failedSends++;
                log.warn("Reminder email failed for user_id={} cycle={} attempt={}: {}",
                        user.getId(), cycleDate, record.getAttempts() + failedSends, e.getMessage());
            } catch (jakarta.mail.MessagingException e) {
                // any non-transient mail failure (e.g. bounce / MessageNotSavedException) → close the cycle
                failedSends++;
                permanentRejection = true;
                log.error("Reminder email permanently rejected for user_id={} cycle={}: {}",
                        user.getId(), cycleDate, e.getMessage());
            }
        }

        if (failedSends == 0) {
            // all sends OK → record SENT and advance to the next cycle
            record.setStatus(STATUS_SENT);
            record.setSentAt(LocalDateTime.now(clock));
            deliveryRecordRepository.saveAndFlush(record);
            user.setEmailReminderNextSendDate(cycleDate.plusDays(intervalDays));
            log.info("Reminder email sent for user_id={} cycle={}", user.getId(), cycleDate);
            return;
        }

        record.setAttempts(record.getAttempts() + failedSends);
        if (permanentRejection || record.getAttempts() >= MAX_ATTEMPTS) {
            // cycle closed: status stays FAILED, next send date advances (R7)
            deliveryRecordRepository.saveAndFlush(record);
            user.setEmailReminderNextSendDate(cycleDate.plusDays(intervalDays));
            log.error("Reminder cycle closed for user_id={} cycle={} after {} attempt(s)",
                    user.getId(), cycleDate, record.getAttempts());
        } else {
            // transient failure, attempts below the limit → retry on the next run, no advance
            record.setStatus(STATUS_FAILED);
            deliveryRecordRepository.saveAndFlush(record);
        }
    }

    private Locale localeOf(UserEntity user) {
        String language = user.getLanguage();
        if (language == null || language.isBlank()) {
            return Locale.ENGLISH;
        }
        return new Locale(language);
    }

}
