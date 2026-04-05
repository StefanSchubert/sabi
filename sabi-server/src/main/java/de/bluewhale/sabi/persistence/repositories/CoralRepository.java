/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * SpringDataRepository for CoralEntity.
 *
 * @author Stefan Schubert
 */
public interface CoralRepository extends JpaRepository<CoralEntity, Long> {

    /**
     * Fetches all coral inhabitants of a certain aquarium.
     *
     * @param aquariumId identifies the aquarium
     * @return list of corals belonging to that aquarium
     */
    List<CoralEntity> findCoralEntitiesByAquariumId(Long aquariumId);
}
