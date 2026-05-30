/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for invertebrate catalogue entries.
 * Part of 006-invertebrate-tracking.
 */
public interface InvertebrateCatalogueRepository extends JpaRepository<InvertebrateCatalogueEntity, Long> {
    List<InvertebrateCatalogueEntity> findByScientificNameAndStatusIn(String scientificName, List<String> statuses);
    List<InvertebrateCatalogueEntity> findByStatusOrderByProposalDateDesc(String status);
    Optional<InvertebrateCatalogueEntity> findByIdAndProposerUserId(Long id, Long proposerUserId);
}
