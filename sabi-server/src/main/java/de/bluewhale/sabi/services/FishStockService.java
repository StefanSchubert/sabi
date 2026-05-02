/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishSizeHistoryTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for all use cases around TankFishStock management.
 * Replaces legacy {@link FishService} (002-fish-stock-catalogue).
 *
 * @author Stefan Schubert
 */
public interface FishStockService {

    /**
     * Adds a new fish entry to the user's tank.
     * If fishCatalogueId is set, scientificName and referenceUrl will be snapshotted from the catalogue (FR-009).
     *
     * @param entry       validated fish stock entry
     * @param userEmail   authenticated user email (ownership)
     * @return result with created entry
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> addFishToTank(@NotNull FishStockEntryTo entry, @NotNull String userEmail);

    /**
     * Updates an existing fish entry (only the owner may update).
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> updateFishEntry(@NotNull FishStockEntryTo entry, @NotNull String userEmail);

    /**
     * Records a departure for an active fish (FR-006: departureDate >= addedOn).
     *
     * @param fishId          fish to depart
     * @param departureRecord departure date + reason
     * @param userEmail       authenticated user email
     * @return result with updated entry
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> recordDeparture(@NotNull Long fishId, @NotNull FishDepartureRecordTo departureRecord, @NotNull String userEmail);

    /**
     * Physically deletes a fish entry. Only allowed if no departure record exists (FR-024).
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> deletePhysically(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Returns all fish (active + departed) for the given aquarium, filtered by ownership.
     */
    @NotNull
    List<FishStockEntryTo> getFishForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    /**
     * Returns a single fish entry by ID (with ownership check).
     *
     * @return the entry, or null if not found / not yours
     */
    FishStockEntryTo getFishById(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Deletes the photo of a fish entry (removes file + FishPhotoEntity).
     */
    @Transactional
    ResultTo<FishStockEntryTo> deletePhoto(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Removes the catalogue link from a fish entry (catalogueId → null).
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> removeCatalogueLink(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Stores or updates a photo for a fish entry.
     */
    @NotNull
    @Transactional
    ResultTo<FishStockEntryTo> uploadPhoto(@NotNull Long fishId, @NotNull byte[] bytes, @NotNull String contentType, @NotNull String userEmail);

    /**
     * Returns the raw photo bytes for a fish (FR-025: ownership check).
     *
     * @return byte array; empty if no photo exists
     */
    @NotNull
    byte[] getPhotoBytes(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Returns all size records for a fish, newest first (ownership check).
     */
    @NotNull
    List<FishSizeHistoryTo> getSizeHistory(@NotNull Long fishId, @NotNull String userEmail);

    /**
     * Adds a new size record for an existing fish entry (ownership check).
     */
    @NotNull
    @Transactional
    ResultTo<FishSizeHistoryTo> addSizeRecord(@NotNull Long fishId,
                                              @NotNull FishSizeHistoryTo record,
                                              @NotNull String userEmail);
}


