/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface UserMeasurementReminderRepository extends JpaRepository<UserMeasurementReminderEntity, Long> {

    /**
     * Retrieves measurement Reminder Entity, if it exists.
     * @param userEntity
     * @param unitID
     * @return UserMeasurementReminderEntity
     */
    Optional<UserMeasurementReminderEntity> findTopByUserAndUnitId(@NotNull UserEntity userEntity, @NotNull Integer unitID);

    /**
     * Loads all measurement reminders of a user.
     *
     * <p>Deliberately used by the email reminder service (sabi-69) instead of iterating
     * {@code user.getUserMeasurementReminders()}: derived queries always execute SQL, which
     * bypasses EclipseLink's shared cache that can serve a stale (initialized-empty) lazy
     * collection for the user entity.</p>
     *
     * @param userId the user's primary key
     * @return the user's reminders, possibly empty
     */
    List<UserMeasurementReminderEntity> findByUserId(Long userId);

}
