/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralPolypConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CoralPolypConditionEntity.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralPolypConditionRepository extends JpaRepository<CoralPolypConditionEntity, Long> {

    /** Condition observations for a coral, latest first. */
    List<CoralPolypConditionEntity> findByCoralStockIdOrderByObservedOnDesc(Long coralStockId);

    /** Ownership-scoped lookup: only return record if it belongs to the given coral. */
    Optional<CoralPolypConditionEntity> findByIdAndCoralStockId(Long id, Long coralStockId);
}

