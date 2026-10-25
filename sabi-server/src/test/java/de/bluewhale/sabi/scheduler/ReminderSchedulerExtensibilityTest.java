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
import de.bluewhale.sabi.services.MeasurementReminderEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import java.util.Optional;
import java.util.Properties;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_12_2_2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Extensibility test for the reminder content SPI (sabi-69, T029, SC-006 / FR-011):
 * a second {@link ReminderContentProvider} bean (type "MAINTENANCE", stub content)
 * is registered in the Spring context under its own name. The scheduler must pick it
 * up and process it without any change to {@link ReminderScheduler} or
 * {@link MeasurementReminderContentProvider}.
 *
 * <p>The second provider is added as a named {@code @MockitoBean}: this registers an
 * additional bean of the SPI type without replacing the existing one, exactly like a
 * new {@code @Component} would be picked up in production. (A nested static
 * {@code @Configuration} inside the test class is not usable here — Spring Boot would
 * use it as the context's root configuration instead of the application class.)</p>
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("IntegrationTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReminderSchedulerExtensibilityTest {

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

    /** Second SPI provider, registered under its own name (does NOT replace the real one). */
    @MockitoBean(name = "maintenanceReminderContentProvider")
    ReminderContentProvider maintenanceProvider;

    @MockitoBean
    JavaMailSender mailer;

    @MockitoBean
    Clock clock;

    private List<MimeMessage> sentMessages;

    @BeforeEach
    void setUp() {
        sentMessages = new ArrayList<>();
        given(clock.instant()).willReturn(TODAY.atTime(9, 0).atZone(ZoneOffset.UTC).toInstant());
        given(clock.getZone()).willReturn(ZoneOffset.UTC);

        // The stub "MAINTENANCE" provider always has content and never reads measurements.
        given(maintenanceProvider.type()).willReturn("MAINTENANCE");
        given(maintenanceProvider.buildContent(any(), any())).willAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            return Optional.of(new MeasurementReminderEmail(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    "de",
                    List.of(),
                    "https://sabi-project.net/measurements.xhtml",
                    "https://sabi-project.net/profile.xhtml"));
        });

        // willAnswer (not willReturn): every send must get a FRESH MimeMessage instance.
        given(mailer.createMimeMessage()).willAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doAnswer(invocation -> {
            sentMessages.add(invocation.getArgument(0));
            return null;
        }).when(mailer).send(any(MimeMessage.class));
    }

    // ------------------------------------------------------------------
    // SC-006 / FR-011: a second provider is processed without core changes
    // ------------------------------------------------------------------

    @Test
    void testSecondProviderIsProcessedWithoutCoreChanges() throws Exception {
        // Given: a due user WITHOUT overdue measurement units → the MEASUREMENT
        // provider returns empty, only the MAINTENANCE stub has content
        UserEntity user = createDueUser("ext_maint@example.com", false);

        // When
        reminderScheduler.run();

        // Then: exactly one email — delivered by the MAINTENANCE stub provider
        assertEquals(1, sentMessages.size());
        assertEquals("ext_maint@example.com", recipientOf(sentMessages.get(0)));
        verify(maintenanceProvider).buildContent(any(), any());

        ReminderDeliveryRecordEntity record = deliveryRecordRepository
                .findByUserIdAndCycleDate(user.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_SENT, record.getStatus());
        assertNotNull(record.getSentAt());

        // the cycle advances exactly like for any other provider
        assertEquals(TODAY.plusDays(7), reload(user).getEmailReminderNextSendDate());
    }

    @Test
    void testBothProvidersContributeToTheSameCycle() throws Exception {
        // Given: a due user WITH an overdue unit → both providers return content
        UserEntity user = createDueUser("ext_both@example.com", true);

        // When
        reminderScheduler.run();

        // Then: one email per provider payload — 2 emails for the same user + cycle
        assertEquals(2, sentMessages.size());
        for (MimeMessage message : sentMessages) {
            assertEquals("ext_both@example.com", recipientOf(message));
        }

        // a single delivery record per user + cycle, status SENT
        ReminderDeliveryRecordEntity record = deliveryRecordRepository
                .findByUserIdAndCycleDate(user.getId(), TODAY).orElseThrow();
        assertEquals(ReminderScheduler.STATUS_SENT, record.getStatus());
        assertEquals(TODAY.plusDays(7), reload(user).getEmailReminderNextSendDate());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private UserEntity createDueUser(String email, boolean withOverdueReminder) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setPassword("");
        user.setValidateToken("");
        user.setValidated(true);
        user.setLanguage("de");
        user.setCountry("DE");
        user.setEmailReminderEnabled(true);
        user.setEmailReminderIntervalDays(7);
        user.setEmailReminderNextSendDate(TODAY);
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
