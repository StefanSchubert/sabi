/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.scheduler;

import de.bluewhale.sabi.persistence.model.ReminderDeliveryRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import de.bluewhale.sabi.persistence.repositories.ReminderDeliveryRecordRepository;
import de.bluewhale.sabi.persistence.repositories.UserMeasurementReminderRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_12_2_2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;

/**
 * Scheduler tests for the email measurement reminder (sabi-69, T011):
 * exactly one localized email per user + cycle (SC-002), idempotency of duplicate
 * runs (SC-003), retry semantics with 3 attempts (spec edge case) and per-user
 * transaction isolation (a failing recipient never blocks other users).
 *
 * <p>Real database + real scheduler; only the {@code JavaMailSender} and the
 * {@code Clock} are mocked.</p>
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReminderSchedulerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final int SEED_UNIT_ID = 1; // "KH" from V1_0_0_2__basicData.sql

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_12_2_2);

    @Autowired
    ReminderScheduler reminderScheduler;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserMeasurementReminderRepository userMeasurementReminderRepository;

    @Autowired
    ReminderDeliveryRecordRepository deliveryRecordRepository;

    @MockitoBean
    JavaMailSender mailer;

    @MockitoBean
    Clock clock;

    private List<MimeMessage> sentMessages;
    private Set<String> failingRecipients;

    @BeforeEach
    void setUp() {
        sentMessages = new ArrayList<>();
        failingRecipients = Set.of();
        given(clock.instant()).willReturn(TODAY.atTime(9, 0).atZone(ZoneOffset.UTC).toInstant());
        given(clock.getZone()).willReturn(ZoneOffset.UTC);

        // willAnswer (not willReturn): every send must get a FRESH MimeMessage instance.
        // A single shared instance would be re-filled by each send, so the recorded
        // "failed" message of one recipient would show the next recipient's address.
        given(mailer.createMimeMessage()).willAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(invocation -> {
            MimeMessage message = invocation.getArgument(0);
            String to = message.getAllRecipients()[0].toString();
            if (failingRecipients.contains(to)) {
                throw new MailSendException("SMTP server unavailable");
            }
            // only successfully delivered messages are recorded (a real SMTP server
            // would not have accepted the failed one)
            sentMessages.add(message);
            return null;
        }).when(mailer).send(any(MimeMessage.class));
    }

    // ------------------------------------------------------------------
    // (a) SC-002: exactly one email per due user + cycle
    // ------------------------------------------------------------------

    @Test
    void testDueUserWithOverdueUnits_receivesExactlyOneEmail_andCycleAdvances() throws Exception {
        // Given
        UserEntity dueUser = createReminderUser("sched_ok@example.com", true, TODAY, true);
        UserEntity noOverdueUser = createReminderUser("sched_empty@example.com", true, TODAY, false);
        UserEntity disabledUser = createReminderUser("sched_disabled@example.com", false, null, false);

        // When
        reminderScheduler.run();

        // Then: exactly one email, for the overdue user (SC-002)
        assertEquals(1, sentMessages.size());
        assertEquals("sched_ok@example.com", sentMessages.get(0).getAllRecipients()[0].toString());

        ReminderDeliveryRecordEntity record = deliveryRecordRepository
                .findByUserIdAndCycleDate(dueUser.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_SENT, record.getStatus());
        assertNotNull(record.getSentAt());

        assertEquals(TODAY.plusDays(7), reload(dueUser).getEmailReminderNextSendDate());

        // R6: empty content → no record, but the cycle still advances
        assertTrue(deliveryRecordRepository.findByUserIdAndCycleDate(noOverdueUser.getId(), TODAY).isEmpty());
        assertEquals(TODAY.plusDays(7), reload(noOverdueUser).getEmailReminderNextSendDate());

        // disabled user untouched (FR-010)
        assertNull(reload(disabledUser).getEmailReminderNextSendDate());
    }

    // ------------------------------------------------------------------
    // (b) SC-003: duplicate runs never send a second email for the same cycle
    // ------------------------------------------------------------------

    @Test
    void testSecondRunSameDay_sendsNoSecondEmail() {
        // Given
        UserEntity user = createReminderUser("sched_idem@example.com", true, TODAY, true);

        // When: first run sends the email and advances the cycle
        reminderScheduler.run();
        assertEquals(1, sentMessages.size());

        // When: second run on the same day – user is no longer due
        reminderScheduler.run();

        // Then: still exactly one email (SC-003)
        assertEquals(1, sentMessages.size());
    }

    @Test
    void testSentRecordPresent_duplicateRunIsSkipped() {
        // Given: a SENT record exists for the cycle, but nextSendDate still points to it
        // (simulates a duplicate run before the advance was committed)
        UserEntity user = createReminderUser("sched_skip@example.com", true, TODAY, true);
        reminderScheduler.run();
        assertEquals(1, sentMessages.size());

        user = reload(user);
        user.setEmailReminderNextSendDate(TODAY); // force "still due" for the same cycle
        userRepository.saveAndFlush(user);

        // When
        reminderScheduler.run();

        // Then: the SENT record makes the run a no-op – no second email
        assertEquals(1, sentMessages.size());
    }

    // ------------------------------------------------------------------
    // (c) transient SMTP failure: retry, close the cycle after 3 attempts
    // ------------------------------------------------------------------

    @Test
    void testTransientSmtpFailure_retriesAndClosesAfterThreeAttempts() {
        // Given
        UserEntity user = createReminderUser("sched_fail@example.com", true, TODAY, true);
        failingRecipients = Set.of("sched_fail@example.com");

        // When / Then: run 1 → FAILED, attempts=1, nextSendDate NOT advanced
        reminderScheduler.run();
        ReminderDeliveryRecordEntity record = deliveryRecordRepository
                .findByUserIdAndCycleDate(user.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_FAILED, record.getStatus());
        assertEquals(1, record.getAttempts());
        assertNull(record.getSentAt());
        assertEquals(TODAY, reload(user).getEmailReminderNextSendDate(), "no advance on transient failure");

        // run 2 → attempts=2, still not advanced
        reminderScheduler.run();
        record = deliveryRecordRepository.findByUserIdAndCycleDate(user.getId(), TODAY).orElseThrow();
        assertEquals(2, record.getAttempts());
        assertEquals(TODAY, reload(user).getEmailReminderNextSendDate());

        // run 3 → attempts=3 → cycle closed, next send date advances
        reminderScheduler.run();
        record = deliveryRecordRepository.findByUserIdAndCycleDate(user.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.MAX_ATTEMPTS, record.getAttempts());
        assertEquals(TODAY.plusDays(7), reload(user).getEmailReminderNextSendDate(), "cycle closed after 3 attempts");

        // run 4 → user no longer due, nothing happens
        int emailsSoFar = sentMessages.size();
        reminderScheduler.run();
        assertEquals(emailsSoFar, sentMessages.size());
    }

    // ------------------------------------------------------------------
    // (d) one failing recipient must not block the other users
    // ------------------------------------------------------------------

    @Test
    void testFailingRecipient_doesNotBlockOtherUsers() throws Exception {
        // Given
        UserEntity failingUser = createReminderUser("sched_block1@example.com", true, TODAY, true);
        UserEntity healthyUser = createReminderUser("sched_block2@example.com", true, TODAY, true);
        failingRecipients = Set.of("sched_block1@example.com");

        // When
        reminderScheduler.run();

        // Then: the healthy user got his email, the failing one is recorded as FAILED
        List<String> recipients = new ArrayList<>();
        for (MimeMessage message : sentMessages) {
            recipients.add(recipientOf(message));
        }
        assertEquals(List.of("sched_block2@example.com"), recipients);

        ReminderDeliveryRecordEntity failed = deliveryRecordRepository
                .findByUserIdAndCycleDate(failingUser.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_FAILED, failed.getStatus());

        ReminderDeliveryRecordEntity sent = deliveryRecordRepository
                .findByUserIdAndCycleDate(healthyUser.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_SENT, sent.getStatus());
        assertEquals(TODAY.plusDays(7), reload(healthyUser).getEmailReminderNextSendDate());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private UserEntity createReminderUser(String email, boolean enabled, LocalDate nextSendDate,
                                          boolean withOverdueReminder) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setPassword("");
        user.setValidateToken("");
        user.setValidated(true);
        user.setLanguage("de");
        user.setCountry("DE");
        user.setEmailReminderEnabled(enabled);
        user.setEmailReminderIntervalDays(7);
        user.setEmailReminderNextSendDate(nextSendDate);
        UserEntity saved = userRepository.saveAndFlush(user);

        if (withOverdueReminder) {
            UserMeasurementReminderEntity reminder = new UserMeasurementReminderEntity();
            reminder.setUser(saved);
            reminder.setUnitId(SEED_UNIT_ID);
            reminder.setPastdays(7);
            reminder.setActive(true);
            saved.getUserMeasurementReminders().add(userMeasurementReminderRepository.saveAndFlush(reminder));
        }
        return saved;
    }

    private UserEntity reload(UserEntity user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    private static String recipientOf(MimeMessage message) throws Exception {
        return message.getAllRecipients()[0].toString();
    }

}
