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
 * API-Gateway interface for invertebrate stock management (webclient → backend).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public interface InvertebrateStockService {

    @NotNull List<InvertebrateStockEntryTo> getInvertebratesForTank(@NotNull Long aquariumId, @NotNull String token) throws BusinessException;

    InvertebrateStockEntryTo getInvertebrateById(@NotNull Long invertebrateId, @NotNull String token) throws BusinessException;

    ResultTo<InvertebrateStockEntryTo> addInvertebrate(@NotNull InvertebrateStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<InvertebrateStockEntryTo> updateInvertebrate(@NotNull InvertebrateStockEntryTo entry, @NotNull String token) throws BusinessException;

    ResultTo<InvertebrateStockEntryTo> recordDeparture(@NotNull Long invertebrateId, @NotNull InvertebrateDepartureRecordTo record, @NotNull String token) throws BusinessException;

    void deleteInvertebrate(@NotNull Long invertebrateId, @NotNull String token) throws BusinessException;

    ResultTo<InvertebrateStockEntryTo> removeCatalogueLink(@NotNull Long invertebrateId, @NotNull String token) throws BusinessException;

    void uploadPhoto(@NotNull Long invertebrateId, @NotNull byte[] bytes, @NotNull String contentType, @NotNull String token) throws BusinessException;

    byte[] getPhoto(@NotNull Long invertebrateId, @NotNull String token) throws BusinessException;
}
