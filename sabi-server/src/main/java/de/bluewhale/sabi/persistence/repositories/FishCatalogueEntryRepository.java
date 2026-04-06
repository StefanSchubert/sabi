/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for FishCatalogueEntryEntity.
 * Provides search and UGC-workflow queries.
 *
 * @author Stefan Schubert
 */
public interface FishCatalogueEntryRepository extends JpaRepository<FishCatalogueEntryEntity, Long> {

    /**
     * Search by query in scientific_name or i18n common_name.
     * Visibility: PUBLIC + own PENDING entries (SC-009, FR-020).
     */
    @Query("SELECT DISTINCT c FROM FishCatalogueEntryEntity c " +
           "LEFT JOIN c.i18nEntries i " +
           "WHERE (c.status = 'PUBLIC' OR (c.status = 'PENDING' AND c.proposerUserId = :userId)) " +
           "AND (LOWER(c.scientificName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR (i.languageCode = :lang AND LOWER(i.commonName) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<FishCatalogueEntryEntity> searchByQueryAndLang(
            @Param("query") String query,
            @Param("lang") String lang,
            @Param("userId") Long userId);

    /**
     * Admin: all pending proposals sorted by submission date ASC.
     */
    List<FishCatalogueEntryEntity> findAllByStatusOrderByProposalDateAsc(String status);

    /**
     * Duplicate check for scientific name among PENDING and PUBLIC entries (FR-015, FR-012).
     */
    boolean existsByScientificNameAndStatusIn(String scientificName, List<String> statuses);

}

