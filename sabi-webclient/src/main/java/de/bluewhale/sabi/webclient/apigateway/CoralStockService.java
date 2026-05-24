/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * API-Gateway interface for coral stock management (webclient → backend).
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralStockService {

    @NotNull List<CoralStockEntryTo> getCoralsForTank(@NotNull Long aquariumId, @NotNull String token) throws BusinessException;

    CoralStockEntryTo getCoralById(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    ResultTo<CoralStockEntryTo> addCoral(@NotNull CoralStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<CoralStockEntryTo> updateCoral(@NotNull CoralStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<CoralStockEntryTo> recordDeparture(@NotNull Long coralId, @NotNull CoralDepartureRecordTo record, @NotNull String token) throws BusinessException;

    void deleteCoral(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    ResultTo<CoralStockEntryTo> removeCatalogueLink(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    void uploadPhoto(@NotNull Long coralId, @NotNull byte[] bytes, @NotNull String contentType, @NotNull String token) throws BusinessException;

    byte[] getPhoto(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    @NotNull List<CoralGrowthHistoryTo> getGrowthHistory(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    ResultTo<CoralGrowthHistoryTo> addGrowthRecord(@NotNull Long coralId, @NotNull CoralGrowthHistoryTo record, @NotNull String token) throws BusinessException;

    ResultTo<CoralGrowthHistoryTo> updateGrowthRecord(@NotNull Long coralId, @NotNull CoralGrowthHistoryTo record, @NotNull String token) throws BusinessException;

    void deleteGrowthRecord(@NotNull Long coralId, @NotNull Long recordId, @NotNull String token) throws BusinessException;

    @NotNull List<CoralPolypConditionTo> getPolypHistory(@NotNull Long coralId, @NotNull String token) throws BusinessException;

    ResultTo<CoralPolypConditionTo> addPolypObservation(@NotNull Long coralId, @NotNull CoralPolypConditionTo obs, @NotNull String token) throws BusinessException;

    ResultTo<CoralPolypConditionTo> updatePolypObservation(@NotNull Long coralId, @NotNull CoralPolypConditionTo obs, @NotNull String token) throws BusinessException;

    void deletePolypObservation(@NotNull Long coralId, @NotNull Long recordId, @NotNull String token) throws BusinessException;
}

