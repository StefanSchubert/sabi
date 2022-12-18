/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
