/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.InvertebratePhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository for InvertebratePhotoEntity.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public interface InvertebratePhotoRepository extends JpaRepository<InvertebratePhotoEntity, Long> {

    /** Find the photo metadata for an invertebrate stock entry (at most one per entry). */
    Optional<InvertebratePhotoEntity> findByInvertebrateStockId(Long invertebrateStockId);

    @Transactional
    void deleteByInvertebrateStockId(Long invertebrateStockId);
}
