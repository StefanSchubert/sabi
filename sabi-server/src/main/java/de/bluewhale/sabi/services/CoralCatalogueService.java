/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.CoralCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for coral catalogue management with UGC workflow.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralCatalogueService {

    /**
     * Search catalogue by scientific or common name (FULLTEXT, min 2 chars, FR-030).
     * Returns PUBLIC entries visible to all + own PENDING entries for the caller.
     */
    @NotNull
    List<CoralCatalogueSearchResultTo> search(@NotNull String query, String lang, @NotNull String userEmail);

    /**
     * List all catalogue entries: PUBLIC for everyone + own PENDING entries (FR-024 visibility).
     */
    @NotNull
    List<CoralCatalogueEntryTo> listAll(String userEmail, String lang);

    /**
     * Get a single catalogue entry by ID (PUBLIC or own PENDING only).
     */
    CoralCatalogueEntryTo getById(@NotNull Long id, @NotNull String userEmail);

    /**
     * Propose a new catalogue entry (status = PENDING).
     * Non-blocking duplicate warning in ResultTo when scientific name already exists (FR-025).
     */
    @NotNull
    @Transactional
    ResultTo<CoralCatalogueEntryTo> proposeEntry(@NotNull CoralCatalogueEntryTo entry, @NotNull String userEmail);

    /**
     * Update an existing entry (proposer or admin).
     * Re-evaluates duplicate check on name change (FR-029).
     * REJECTED entries return NOT_YOUR_ENTRY for non-admin (FR-029).
     */
    @NotNull
    @Transactional
    ResultTo<CoralCatalogueEntryTo> updateEntry(@NotNull CoralCatalogueEntryTo entry, @NotNull String userEmail);
}

