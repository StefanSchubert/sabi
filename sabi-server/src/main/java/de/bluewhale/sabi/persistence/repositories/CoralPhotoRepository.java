/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for CoralPhotoEntity.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralPhotoRepository extends JpaRepository<CoralPhotoEntity, Long> {

    /** Find the photo metadata for a coral stock entry (at most one per coral). */
    Optional<CoralPhotoEntity> findByCoralStockId(Long coralStockId);
}

