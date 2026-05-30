/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.InvertebrateCatalogueEntryTo;
import de.bluewhale.sabi.model.InvertebrateCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;

import java.util.List;

/**
 * API-Gateway interface for invertebrate catalogue operations (webclient → backend).
 * Part of 006-invertebrate-tracking.
 */
public interface InvertebrateCatalogueService {
    List<InvertebrateCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException;
    List<InvertebrateCatalogueSearchResultTo> listAll(String lang, String token) throws BusinessException;
    InvertebrateCatalogueEntryTo getById(Long id, String token) throws BusinessException;
    ResultTo proposeEntry(InvertebrateCatalogueEntryTo entry, String token) throws BusinessException;
    ResultTo updateEntry(InvertebrateCatalogueEntryTo entry, String token) throws BusinessException;
}
