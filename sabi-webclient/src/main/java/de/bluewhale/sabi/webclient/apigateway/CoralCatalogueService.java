/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.CoralCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;

import java.util.List;

/**
 * API-Gateway interface for coral catalogue operations (webclient → backend).
 * Part of 005-coral-stock.
 */
public interface CoralCatalogueService {
    List<CoralCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException;
    List<CoralCatalogueSearchResultTo> listAll(String lang, String token) throws BusinessException;
    CoralCatalogueEntryTo getById(Long id, String token) throws BusinessException;
    ResultTo proposeEntry(CoralCatalogueEntryTo entry, String token) throws BusinessException;
    ResultTo updateEntry(CoralCatalogueEntryTo entry, String token) throws BusinessException;
}

