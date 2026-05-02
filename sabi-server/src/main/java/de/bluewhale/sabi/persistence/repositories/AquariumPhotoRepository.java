/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for aquarium photo metadata.
 *
 * @author Stefan Schubert
 */
public interface AquariumPhotoRepository extends JpaRepository<AquariumPhotoEntity, Long> {

    Optional<AquariumPhotoEntity> findByAquariumId(Long aquariumId);

    void deleteByAquariumId(Long aquariumId);

}

