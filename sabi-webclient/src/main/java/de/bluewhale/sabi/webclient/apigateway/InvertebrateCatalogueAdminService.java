/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.InvertebrateCatalogueEntryTo;
import de.bluewhale.sabi.model.ResultTo;

import java.util.List;

/**
 * API-Gateway interface for admin invertebrate catalogue operations.
 * Part of 006-invertebrate-tracking.
 */
public interface InvertebrateCatalogueAdminService {
    List<InvertebrateCatalogueEntryTo> listPending(String token) throws BusinessException;
    /** Fetch ALL catalogue entries (any status) for the admin catalogue browser. */
    List<InvertebrateCatalogueEntryTo> listAll(String token) throws BusinessException;
    ResultTo approve(Long id, String token) throws BusinessException;
    ResultTo reject(Long id, String token) throws BusinessException;
    ResultTo adminUpdate(Long id, InvertebrateCatalogueEntryTo entry, String token) throws BusinessException;
}
