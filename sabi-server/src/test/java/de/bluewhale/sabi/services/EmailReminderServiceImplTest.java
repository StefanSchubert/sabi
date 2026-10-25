/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.UserProfileMessageCodes;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UnitRepository;
import de.bluewhale.sabi.persistence.repositories.UserMeasurementReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_12_2_2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * State-machine tests for the email measurement reminder (sabi-69):
 * enable/disable transitions, interval validation (1-30) and the overdue rule (FR-008).
 *
 * <p>Repositories are mocked; the Testcontainer only satisfies Spring context startup.</p>
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmailReminderServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final int UNIT_ID = 1;

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_12_2_2);

    @Autowired
    EmailReminderServiceImpl emailReminderService;

    @MockitoBean
    MeasurementRepository measurementRepository;

    @MockitoBean
    UnitRepository unitRepository;

    @MockitoBean
    UserMeasurementReminderRepository userMeasurementReminderRepository;

    @MockitoBean
    Clock clock;

    @BeforeEach
    void setUpClock() {
        given(clock.instant()).willReturn(TODAY.atTime(9, 0).atZone(ZoneOffset.UTC).toInstant());
        given(clock.getZone()).willReturn(ZoneOffset.UTC);
    }

    // ------------------------------------------------------------------
    // T012: enable transitions (FR-001, SC-001)
    // ------------------------------------------------------------------

    @Test
    void testEnableWithOverdueUnits_setsNextSendDateToToday() throws Exception {
        // Given: disabled user, one active in-app reminder that was never measured (→ overdue)
        UserEntity user = createUser(false, null, null);
        createActiveReminder(user, UNIT_ID, 7);
        given(measurementRepository.findTopByUserAndUnitIdOrderByMeasuredOnDesc(user, UNIT_ID))
                .willReturn(Optional.empty());

        // When
        emailReminderService.applyProfileChange(user, true, 7);

        // Then (FR-001): overdue units → send on the day of enablement
        assertTrue(user.isEmailReminderEnabled());
        assertEquals(TODAY, user.getEmailReminderNextSendDate());
    }

    @Test
    void testEnableWithoutOverdueUnits_setsNextSendDateToTodayPlusInterval() throws Exception {
        // Given: disabled user, one active in-app reminder with a recent measurement (→ not overdue)
        UserEntity user = createUser(false, null, null);
        createActiveReminder(user, UNIT_ID, 7);
        given(measurementRepository.findTopByUserAndUnitIdOrderByMeasuredOnDesc(user, UNIT_ID))
                .willReturn(Optional.of(createMeasurement(TODAY.atTime(8, 0), 5.0f)));

        // When
        emailReminderService.applyProfileChange(user, true, 7);

        // Then: first send at the next full cycle
        assertTrue(user.isEmailReminderEnabled());
        assertEquals(TODAY.plusDays(7), user.getEmailReminderNextSendDate());
    }

    @Test
    void testEnableWithNullInterval_usesDefaultSevenDays() throws Exception {
        // Given: disabled user, no overdue units, interval omitted (old client)
        UserEntity user = createUser(false, null, null);

        // When
        emailReminderService.applyProfileChange(user, true, null);

        // Then: default interval 7, first send next cycle
        assertEquals(7, user.getEmailReminderIntervalDays());
        assertEquals(TODAY.plusDays(7), user.getEmailReminderNextSendDate());
    }

    // ------------------------------------------------------------------
    // T022: interval validation (FR-002, US-2 scenario 3)
    // ------------------------------------------------------------------

    @Test
    void testEnableWithIntervalZero_rejectsWholeUpdate() throws Exception {
        UserEntity user = createUser(false, null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailReminderService.applyProfileChange(user, true, 0));

        assertEquals(UserProfileMessageCodes.EMAIL_REMINDER_INTERVAL_OUT_OF_RANGE,
                exception.getMessages().get(0).getCode());
        // the profile update must be left untouched
        assertFalse(user.isEmailReminderEnabled());
        assertNull(user.getEmailReminderNextSendDate());
    }

    @Test
    void testEnableWithInterval31_rejectsWholeUpdate() throws Exception {
        UserEntity user = createUser(false, null, null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailReminderService.applyProfileChange(user, true, 31));

        assertEquals(UserProfileMessageCodes.EMAIL_REMINDER_INTERVAL_OUT_OF_RANGE,
                exception.getMessages().get(0).getCode());
        assertFalse(user.isEmailReminderEnabled());
    }

    @Test
    void testEnableWithBoundaryIntervalOne_isAccepted() throws Exception {
        UserEntity user = createUser(false, null, null);

        emailReminderService.applyProfileChange(user, true, 1);

        assertEquals(1, user.getEmailReminderIntervalDays());
        assertTrue(user.isEmailReminderEnabled());
    }

    @Test
    void testEnableWithBoundaryInterval30_isAccepted() throws Exception {
        UserEntity user = createUser(false, null, null);

        emailReminderService.applyProfileChange(user, true, 30);

        assertEquals(30, user.getEmailReminderIntervalDays());
        assertTrue(user.isEmailReminderEnabled());
    }

    // ------------------------------------------------------------------
    // T023: interval change + disable (FR-009, FR-010)
    // ------------------------------------------------------------------

    @Test
    void testIntervalChangeWhileEnabled_recomputesFromNow() throws Exception {
        // Given: enabled user with interval 7 and a pending next send date
        UserEntity user = createUser(true, 7, TODAY.plusDays(2));

        // When: interval changed to 14
        emailReminderService.applyProfileChange(user, true, 14);

        // Then (FR-009): next send date recomputed from today
        assertEquals(14, user.getEmailReminderIntervalDays());
        assertEquals(TODAY.plusDays(14), user.getEmailReminderNextSendDate());
    }

    @Test
    void testReEnableWithSameInterval_keepsNextSendDate() throws Exception {
        // Given: enabled user, interval unchanged (7)
        UserEntity user = createUser(true, 7, TODAY.plusDays(3));

        // When
        emailReminderService.applyProfileChange(user, true, 7);

        // Then: no recompute, next send date untouched
        assertEquals(TODAY.plusDays(3), user.getEmailReminderNextSendDate());
    }

    @Test
    void testDisable_clearsFlagAndNextSendDate() throws Exception {
        // Given: enabled user with a pending next send date
        UserEntity user = createUser(true, 7, TODAY.plusDays(2));

        // When (FR-010)
        emailReminderService.applyProfileChange(user, false, null);

        // Then: flag off, next send date NULL → the due-user query can no longer match
        assertFalse(user.isEmailReminderEnabled());
        assertNull(user.getEmailReminderNextSendDate());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private UserEntity createUser(boolean enabled, Integer intervalDays, LocalDate nextSendDate) {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setEmail("reminder-test@example.com");
        user.setUsername("reminderTest");
        user.setLanguage("de");
        user.setCountry("DE");
        user.setEmailReminderEnabled(enabled);
        user.setEmailReminderIntervalDays(intervalDays != null ? intervalDays : 7);
        user.setEmailReminderNextSendDate(nextSendDate);
        return user;
    }

    private UserMeasurementReminderEntity createActiveReminder(UserEntity user, int unitId, int pastdays) {
        UserMeasurementReminderEntity reminder = new UserMeasurementReminderEntity();
        reminder.setUser(user);
        reminder.setUnitId(unitId);
        reminder.setPastdays(pastdays);
        reminder.setActive(true);
        // The service reads reminders via the repository query (not the lazy user collection),
        // so the mock must serve them from findByUserId.
        given(userMeasurementReminderRepository.findByUserId(user.getId())).willReturn(List.of(reminder));
        return reminder;
    }

    private MeasurementEntity createMeasurement(LocalDateTime measuredOn, float value) {
        MeasurementEntity measurement = new MeasurementEntity();
        measurement.setMeasuredOn(measuredOn);
        measurement.setMeasuredValue(value);
        return measurement;
    }

}
