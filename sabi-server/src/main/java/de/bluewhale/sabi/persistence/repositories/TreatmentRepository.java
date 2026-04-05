/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.TreatmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * SpringDataRepository for TreatmentEntity.
 *
 * @author Stefan Schubert
 */
public interface TreatmentRepository extends JpaRepository<TreatmentEntity, Long> {

    /**
     * Fetches all treatments applied to a certain aquarium.
     *
     * @param aquariumId identifies the aquarium
     * @return list of treatments belonging to that aquarium
     */
    List<TreatmentEntity> findTreatmentEntitiesByAquariumId(Long aquariumId);
}
