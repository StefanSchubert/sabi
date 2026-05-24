/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralCatalogueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CoralCatalogueEntity with UGC workflow support.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralCatalogueRepository extends JpaRepository<CoralCatalogueEntity, Long> {

    /**
     * Duplicate-name warning check (FR-025): find active entries with the given scientific name.
     */
    List<CoralCatalogueEntity> findByScientificNameAndStatusIn(String scientificName, List<String> statuses);

    /**
     * Admin queue: list all proposals of a given status sorted by submission date descending (FR-031).
     */
    List<CoralCatalogueEntity> findByStatusOrderByProposalDateDesc(String status);

    /**
     * Creator ownership check: verify caller is the proposer.
     */
    Optional<CoralCatalogueEntity> findByIdAndProposerUserId(Long id, Long proposerUserId);
}
