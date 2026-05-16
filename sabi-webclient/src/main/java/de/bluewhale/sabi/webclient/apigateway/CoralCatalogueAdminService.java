/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.ResultTo;

import java.util.List;

/**
 * API-Gateway interface for admin coral catalogue operations.
 * Part of 005-coral-stock.
 */
public interface CoralCatalogueAdminService {
    List<CoralCatalogueEntryTo> listPending(String token) throws BusinessException;
    /** Fetch ALL catalogue entries (any status) for the admin catalogue browser. */
    List<CoralCatalogueEntryTo> listAll(String token) throws BusinessException;
    ResultTo approve(Long id, String token) throws BusinessException;
    ResultTo reject(Long id, String token) throws BusinessException;
    ResultTo adminUpdate(Long id, CoralCatalogueEntryTo entry, String token) throws BusinessException;
}

