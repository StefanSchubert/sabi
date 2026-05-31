/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for invertebrate catalogue management.
 * Part of 006-invertebrate-tracking.
 */
public interface InvertebrateCatalogueService {
    @NotNull List<InvertebrateCatalogueSearchResultTo> search(@NotNull String query, String lang, @NotNull String userEmail);
    @NotNull List<InvertebrateCatalogueEntryTo> listAll(String userEmail, String lang);
    InvertebrateCatalogueEntryTo getById(@NotNull Long id, @NotNull String userEmail);
    @NotNull @Transactional ResultTo<InvertebrateCatalogueEntryTo> proposeEntry(@NotNull InvertebrateCatalogueEntryTo entry, @NotNull String userEmail);
    @NotNull @Transactional ResultTo<InvertebrateCatalogueEntryTo> updateEntry(@NotNull InvertebrateCatalogueEntryTo entry, @NotNull String userEmail);
    @Transactional ResultTo<InvertebrateCatalogueEntryTo> approveEntry(Long id, String adminEmail);
    @Transactional ResultTo<InvertebrateCatalogueEntryTo> rejectEntry(Long id, String adminEmail);
    List<InvertebrateCatalogueEntryTo> listPending(String adminEmail);
    List<InvertebrateCatalogueEntryTo> listAllForAdmin(String adminEmail);
}
