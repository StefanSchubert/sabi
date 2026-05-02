/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishSizeHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for fish size history records.
 * Part of 002-fish-stock-catalogue.
 */
public interface FishSizeHistoryRepository extends JpaRepository<FishSizeHistoryEntity, Long> {

    /** Returns all size records for a fish, most recent first. */
    List<FishSizeHistoryEntity> findByFishIdOrderByMeasuredOnDesc(Long fishId);

    /** Returns true if the fish has at least one size record. */
    boolean existsByFishId(Long fishId);
}
