/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralCatalogueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SpringDataRepository for CoralCatalogueEntity.
 *
 * @author Stefan Schubert
 */
public interface CoralCatalogueRepository extends JpaRepository<CoralCatalogueEntity, Long> {

    // Standard findById() from JpaRepository is sufficient.
}
