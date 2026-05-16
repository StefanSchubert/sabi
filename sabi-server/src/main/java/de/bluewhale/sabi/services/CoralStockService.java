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
 * Service for all use cases around coral stock management.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralStockService {

    /** Returns all corals (active + departed) for the given aquarium, filtered by ownership. */
    @NotNull
    List<CoralStockEntryTo> getCoralsForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    /** Returns a single coral by ID with ownership check. */
    CoralStockEntryTo getCoralById(@NotNull Long coralId, @NotNull String userEmail);

    /** Adds a new coral entry to a marine tank (C-8: marine-only guard). */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> addCoralToTank(@NotNull CoralStockEntryTo entry, @NotNull String userEmail);

    /** Updates an existing coral entry (only owner may update). */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> updateCoralEntry(@NotNull CoralStockEntryTo entry, @NotNull String userEmail);

    /**
     * Physically deletes a coral entry.
     * Returns CORAL_HAS_DEPARTURE_RECORD if a departure record exists (FR-012).
     */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> deletePhysically(@NotNull Long coralId, @NotNull String userEmail);

    /**
     * Records a departure for an active coral.
     * Validates departureDate >= addedOn (FR-006).
     */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> recordDeparture(@NotNull Long coralId,
                                                @NotNull CoralDepartureRecordTo record,
                                                @NotNull String userEmail);

    /** Removes the catalogue link from a coral entry (coralCatalogueId → null, FR-010). */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> removeCatalogueLink(@NotNull Long coralId, @NotNull String userEmail);

    /** Stores or replaces a photo for a coral entry (5 MB limit, magic-byte validation). */
    @NotNull
    @Transactional
    ResultTo<CoralStockEntryTo> uploadPhoto(@NotNull Long coralId, @NotNull MultipartFile file, @NotNull String userEmail) throws IOException;

    /** Returns the raw photo bytes for a coral (ownership check). */
    @NotNull
    byte[] getPhotoBytes(@NotNull Long coralId, @NotNull String userEmail);

    /** Deletes the photo of a coral entry (removes file + CoralPhotoEntity). */
    @Transactional
    ResultTo<CoralStockEntryTo> deletePhoto(@NotNull Long coralId, @NotNull String userEmail);

    // --------- Growth History ---------

    /** All growth records for a coral, newest first. */
    @NotNull
    List<CoralGrowthHistoryTo> getGrowthHistory(@NotNull Long coralId, @NotNull String userEmail);

    /** Adds a growth record (type set at creation and immutable, FR-039). */
    @NotNull
    @Transactional
    ResultTo<CoralGrowthHistoryTo> addGrowthRecord(@NotNull Long coralId,
                                                   @NotNull CoralGrowthHistoryTo record,
                                                   @NotNull String userEmail);

    /** Updates date and value only (type is immutable, FR-039). */
    @NotNull
    @Transactional
    ResultTo<CoralGrowthHistoryTo> updateGrowthRecord(@NotNull Long coralId,
                                                      @NotNull CoralGrowthHistoryTo record,
                                                      @NotNull String userEmail);

    /** Deletes a growth record by ID (ownership via parent coral). */
    @NotNull
    @Transactional
    ResultTo<CoralGrowthHistoryTo> deleteGrowthRecord(@NotNull Long coralId,
                                                      @NotNull Long recordId,
                                                      @NotNull String userEmail);

    // --------- Polyp Condition History ---------

    /** All polyp condition records for a coral, newest first. */
    @NotNull
    List<CoralPolypConditionTo> getPolypHistory(@NotNull Long coralId, @NotNull String userEmail);

    /** Adds a polyp condition observation. */
    @NotNull
    @Transactional
    ResultTo<CoralPolypConditionTo> addPolypObservation(@NotNull Long coralId,
                                                        @NotNull CoralPolypConditionTo record,
                                                        @NotNull String userEmail);

    /** Updates observedOn and condition for a polyp observation (FR-040). */
    @NotNull
    @Transactional
    ResultTo<CoralPolypConditionTo> updatePolypObservation(@NotNull Long coralId,
                                                           @NotNull CoralPolypConditionTo record,
                                                           @NotNull String userEmail);

    /** Deletes a polyp observation by ID (ownership via parent coral). */
    @NotNull
    @Transactional
    ResultTo<CoralPolypConditionTo> deletePolypObservation(@NotNull Long coralId,
                                                           @NotNull Long recordId,
                                                           @NotNull String userEmail);

    // --------- Report / Export ---------

    /** Active corals for the public House Reef Report (FR-032). */
    @NotNull
    List<PublicReefReportCoralTo> getActiveCoralsForReport(@NotNull Long aquariumId);

    /** All corals including departed for AI-JSON export (FR-035). */
    @NotNull
    List<CoralExportTo> getCorralsForExport(@NotNull Long aquariumId);
}

