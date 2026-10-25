/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.persistence.model.ReminderDeliveryRecordEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.ReminderDeliveryRecordRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestContainerVersions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persistence-Layer Tests for ReminderDeliveryRecordRepository (sabi-69):
 * unique constraint (user_id, cycle_date), GDPR cascade delete, due-user finder.
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("IntegrationTest")
@Transactional
@DirtiesContext
public class ReminderDeliveryRecordRepositoryTest implements TestContainerVersions {

    private static final LocalDate CYCLE_DATE = LocalDate.of(2026, 10, 24);

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_12_2_2);

    @Autowired
    ReminderDeliveryRecordRepository deliveryRecordRepository;

    @Autowired
    UserRepository userRepository;

    @AfterAll
    static void cleanup() {
        mariaDBContainer.stop();
    }

    private UserEntity createTestUser(String email, boolean reminderEnabled, LocalDate nextSendDate) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setPassword("");
        user.setValidateToken("");
        user.setValidated(true);
        user.setLanguage("de");
        user.setCountry("DE");
        user.setEmailReminderEnabled(reminderEnabled);
        user.setEmailReminderIntervalDays(7);
        user.setEmailReminderNextSendDate(nextSendDate);
        return userRepository.saveAndFlush(user);
    }

    private ReminderDeliveryRecordEntity createDeliveryRecord(UserEntity user, LocalDate cycleDate) {
        ReminderDeliveryRecordEntity record = new ReminderDeliveryRecordEntity();
        record.setUser(user);
        record.setCycleDate(cycleDate);
        record.setStatus("SENT");
        record.setAttempts(0);
        ReminderDeliveryRecordEntity saved = deliveryRecordRepository.saveAndFlush(record);
        // Bidirektionale Beziehung in Sync halten: EclipseLink benötigt die befüllte
        // Collection für CascadeType.REMOVE (Lazy-Collection wäre sonst leer beim Delete).
        user.getReminderDeliveryRecords().add(saved);
        return saved;
    }

    @Test
    @Rollback
    void testFindByUserIdAndCycleDate_findsExistingRecord() {
        // Given
        UserEntity user = createTestUser("rr_find@example.com", true, CYCLE_DATE);
        createDeliveryRecord(user, CYCLE_DATE);

        // When
        Optional<ReminderDeliveryRecordEntity> found =
                deliveryRecordRepository.findByUserIdAndCycleDate(user.getId(), CYCLE_DATE);

        // Then
        assertTrue(found.isPresent());
        assertEquals("SENT", found.get().getStatus());
    }

    @Test
    @Rollback
    void testUniqueConstraint_duplicateUserCycleRejected() {
        // Given
        UserEntity user = createTestUser("rr_unique@example.com", true, CYCLE_DATE);
        createDeliveryRecord(user, CYCLE_DATE);

        // When / Then – a second record for the same (user_id, cycle_date) must fail
        // (EclipseLink wraps constraint violations as DataAccessException subclasses)
        assertThrows(DataAccessException.class, () -> createDeliveryRecord(user, CYCLE_DATE));
    }

    @Test
    @Rollback
    void testCascadeDelete_recordsDeletedWhenUserDeleted() {
        // Given (GDPR, quickstart.md Scenario D)
        UserEntity user = createTestUser("rr_cascade@example.com", true, CYCLE_DATE);
        ReminderDeliveryRecordEntity record = createDeliveryRecord(user, CYCLE_DATE);
        Long recordId = record.getId();

        // When
        userRepository.delete(user);
        userRepository.flush();

        // Then
        assertTrue(deliveryRecordRepository.findById(recordId).isEmpty(),
                "Delivery record should have been cascade-deleted with the user");
    }

    @Test
    @Rollback
    void testDueFinder_returnsOnlyEnabledUsersWithDueDate() {
        // Given
        LocalDate runDate = CYCLE_DATE;
        UserEntity dueUser = createTestUser("rr_due@example.com", true, runDate);
        UserEntity futureUser = createTestUser("rr_future@example.com", true, runDate.plusDays(7));
        // disabled user: next send date must be NULL per data-model validation rule
        UserEntity disabledUser = createTestUser("rr_disabled@example.com", false, null);

        // When
        List<UserEntity> dueUsers =
                userRepository.findByEmailReminderEnabledTrueAndEmailReminderNextSendDateLessThanEqual(runDate);

        // Then
        List<Long> dueIds = dueUsers.stream().map(UserEntity::getId).toList();
        assertTrue(dueIds.contains(dueUser.getId()), "enabled + due user must be returned");
        assertFalse(dueIds.contains(futureUser.getId()), "not-yet-due user must not be returned");
        assertFalse(dueIds.contains(disabledUser.getId()), "disabled user must not be returned (FR-010)");
    }

}
