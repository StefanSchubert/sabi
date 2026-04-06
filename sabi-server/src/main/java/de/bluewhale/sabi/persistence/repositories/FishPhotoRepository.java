/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for fish photo metadata.
 *
 * @author Stefan Schubert
 */
public interface FishPhotoRepository extends JpaRepository<FishPhotoEntity, Long> {

    Optional<FishPhotoEntity> findByFishId(Long fishId);

    void deleteByFishId(Long fishId);

}

