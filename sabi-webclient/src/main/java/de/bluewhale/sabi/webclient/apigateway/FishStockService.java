/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishRoleTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * API-Gateway interface for fish stock management (webclient → backend).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
public interface FishStockService {

    @NotNull List<FishStockEntryTo> getFishForTank(@NotNull Long aquariumId, @NotNull String token) throws BusinessException;

    ResultTo<FishStockEntryTo> addFish(@NotNull FishStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<FishStockEntryTo> updateFish(@NotNull FishStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<FishStockEntryTo> recordDeparture(@NotNull Long fishId, @NotNull FishDepartureRecordTo record, @NotNull String token) throws BusinessException;

    void deleteFish(@NotNull Long fishId, @NotNull String token) throws BusinessException;

    ResultTo<FishStockEntryTo> removeCatalogueLink(@NotNull Long fishId, @NotNull String token) throws BusinessException;

    void uploadPhoto(@NotNull Long fishId, @NotNull byte[] bytes, @NotNull String contentType, @NotNull String token) throws BusinessException;

    byte[] getPhoto(@NotNull Long fishId, @NotNull String token) throws BusinessException;

    /**
     * Returns all available fish roles with localized names/descriptions.
     *
     * @param language ISO 639-1 language code (e.g. "de", "en")
     * @param token    backend auth token
     * @return list of fish roles; empty list on error
     */
    @NotNull List<FishRoleTo> getFishRoles(@NotNull String language, @NotNull String token) throws BusinessException;
}

