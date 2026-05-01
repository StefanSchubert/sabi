/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for fish catalogue management (search, propose, approve, reject).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
public interface FishCatalogueService {

    /**
     * Search for catalogue entries (PUBLIC + own PENDING, FR-020, SC-009).
     * Returns empty list if query is less than 2 characters.
     *
     * @param query        search term (min 2 chars)
     * @param languageCode language for i18n fields (de/en/es/fr/it)
     * @param userEmail    authenticated user email
     * @return list of search results (never null)
     */
    @NotNull
    List<FishCatalogueSearchResultTo> search(@NotNull String query, @NotNull String languageCode, @NotNull String userEmail);

    /**
     * Get a single catalogue entry (PUBLIC or own PENDING).
     *
     * @return the entry or null if not found / not visible to user
     */
    FishCatalogueEntryTo getEntry(@NotNull Long id, @NotNull String userEmail);

    /**
     * Check for duplicate scientific name among PENDING and PUBLIC entries.
     */
    boolean isDuplicateScientificName(@NotNull String scientificName);

    /**
     * Propose a new catalogue entry (sets status=PENDING, proposerUserId, proposalDate).
     * Non-blocking duplicate warning if scientificName already exists (FR-015).
     *
     * @param entryTo   proposal data
     * @param userEmail authenticated user email
     * @return result with CATALOGUE_ENTRY_PROPOSED; WARNING if duplicate
     */
    @NotNull
    @Transactional
    ResultTo<FishCatalogueEntryTo> proposeEntry(@NotNull FishCatalogueEntryTo entryTo, @NotNull String userEmail);

    /**
     * Approve a pending proposal (Admin only, FR-016).
     */
    @NotNull
    @Transactional
    ResultTo<FishCatalogueEntryTo> approveEntry(@NotNull Long id, FishCatalogueEntryTo editedEntry, @NotNull String adminEmail);

    /**
     * Reject a pending proposal (Admin only, FR-017).
     */
    @NotNull
    @Transactional
    ResultTo<FishCatalogueEntryTo> rejectEntry(@NotNull Long id, String reason, @NotNull String adminEmail);

    /**
     * List all pending proposals sorted by proposalDate ASC (Admin only).
     */
    @NotNull
    List<FishCatalogueEntryTo> listPendingProposals(@NotNull String adminEmail);

    /**
     * List all catalogue entries visible to the user: PUBLIC entries + own PENDING entries (FR-018).
     * Used for the catalogue overview after a proposal is submitted.
     *
     * @param userEmail    authenticated user email
     * @param languageCode language for i18n fields (de/en/es/fr/it)
     * @return list of entries (never null)
     */
    @NotNull
    List<FishCatalogueSearchResultTo> listAll(@NotNull String userEmail, @NotNull String languageCode);

    /**
     * Update an existing catalogue entry (FR-019).
     * REJECTED → read-only; PENDING: only creator; PUBLIC: creator + Admin.
     */
    @NotNull
    @Transactional
    ResultTo<FishCatalogueEntryTo> updateEntry(@NotNull FishCatalogueEntryTo entryTo, @NotNull String userEmail);

    /**
     * List ALL catalogue entries regardless of status (Admin only).
     * Used for the full catalogue browser in the admin view.
     *
     * @param adminEmail authenticated admin email
     * @return list of all entries sorted alphabetically by scientific name (never null)
     */
    @NotNull
    List<FishCatalogueEntryTo> listAllForAdmin(@NotNull String adminEmail);
}

