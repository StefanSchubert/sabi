/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.InvertebrateWaterSensitivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for InvertebrateWaterSensitivityEntity.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public interface InvertebrateWaterSensitivityRepository extends JpaRepository<InvertebrateWaterSensitivityEntity, Long> {

    List<InvertebrateWaterSensitivityEntity> findAllByInvertebrateStockId(Long invertebrateStockId);

    @Transactional
    void deleteAllByInvertebrateStockId(Long invertebrateStockId);
}
