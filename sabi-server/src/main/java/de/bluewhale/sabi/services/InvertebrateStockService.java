/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service for all use cases around invertebrate stock management.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public interface InvertebrateStockService {

    /** Returns all invertebrates (active + departed) for the given aquarium, filtered by ownership. */
    @NotNull
    List<InvertebrateStockEntryTo> listForAquarium(@NotNull Long aquariumId, @NotNull String userEmail);

    /** Returns a single invertebrate by ID with ownership check. */
    InvertebrateStockEntryTo getById(@NotNull Long invertebrateId, @NotNull String userEmail);

    /** Adds a new invertebrate entry to a marine tank. */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> create(@NotNull InvertebrateStockEntryTo entry, @NotNull String userEmail);

    /** Updates an existing invertebrate entry (only owner may update). */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> update(@NotNull InvertebrateStockEntryTo entry, @NotNull String userEmail);

    /**
     * Physically deletes an invertebrate entry.
     * Returns INVERT_HAS_DEPARTURE_RECORD if a departure record exists.
     */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> delete(@NotNull Long invertebrateId, @NotNull String userEmail);

    /**
     * Records a departure for an active invertebrate.
     * Validates departedOn >= addedOn.
     */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> recordDeparture(@NotNull Long invertebrateId,
                                                        @NotNull InvertebrateDepartureRecordTo record,
                                                        @NotNull String userEmail);

    /** Removes the catalogue link from an invertebrate entry (invertebrateCatalogueId → null). */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> removeCatalogueLink(@NotNull Long invertebrateId, @NotNull String userEmail);

    /** Stores or replaces a photo for an invertebrate entry (5 MB limit, magic-byte validation). */
    @NotNull
    @Transactional
    ResultTo<InvertebrateStockEntryTo> uploadPhoto(@NotNull Long invertebrateId, @NotNull MultipartFile file, @NotNull String userEmail) throws IOException;

    /** Returns the raw photo bytes for an invertebrate (ownership check). */
    @NotNull
    byte[] getPhotoBytes(@NotNull Long invertebrateId, @NotNull String userEmail);

    /** Deletes the photo of an invertebrate entry (removes file + InvertebratePhotoEntity). */
    @Transactional
    ResultTo<InvertebrateStockEntryTo> deletePhoto(@NotNull Long invertebrateId, @NotNull String userEmail);

    /**
     * Returns currently active invertebrates (no departedOn) for a given aquarium.
     * Used for public reef report integration (006-invertebrate-tracking / US7).
     * No ownership check — called from PublicReportService.
     */
    @NotNull
    List<InvertebrateStockEntryTo> getActiveInvertebratesForReport(@NotNull Long aquariumId);
}
