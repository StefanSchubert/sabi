/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.ReminderDeliveryRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * SpringDataRepository for email reminder delivery records.
 *
 * @author Stefan Schubert
 */
public interface ReminderDeliveryRecordRepository extends JpaRepository<ReminderDeliveryRecordEntity, Long> {

    /**
     * Finds the delivery record for one user and cycle date (idempotency key, FR-004).
     *
     * @param userId    the owning user's ID
     * @param cycleDate the due date of the cycle
     * @return the existing record, if any
     */
    Optional<ReminderDeliveryRecordEntity> findByUserIdAndCycleDate(Long userId, LocalDate cycleDate);

}
