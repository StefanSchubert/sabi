/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * Part of 002-fish-stock-catalogue - T063
 */
package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;

import java.util.List;

/**
 * API-Gateway interface for fish catalogue admin operations.
 */
public interface FishCatalogueAdminService {

    List<FishCatalogueEntryTo> getPendingProposals(String token) throws BusinessException;

    /** Fetch ALL catalogue entries (any status) for the admin catalogue browser. */
    List<FishCatalogueEntryTo> getAllEntries(String token) throws BusinessException;

    FishCatalogueEntryTo approveEntry(Long id, FishCatalogueEntryTo edits, String token) throws BusinessException;

    void rejectEntry(Long id, String reason, String token) throws BusinessException;
}
