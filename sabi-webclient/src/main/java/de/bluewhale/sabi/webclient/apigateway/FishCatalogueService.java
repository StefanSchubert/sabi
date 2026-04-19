/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.apigateway;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.ResultTo;
import java.util.List;
public interface FishCatalogueService {
    List<FishCatalogueSearchResultTo> search(String query, String lang, String token) throws BusinessException;
    List<FishCatalogueSearchResultTo> listAll(String lang, String token) throws BusinessException;
    ResultTo propose(FishCatalogueEntryTo entry, String token) throws BusinessException;
    ResultTo updateEntry(FishCatalogueEntryTo entry, String token) throws BusinessException;
    boolean isDuplicate(String scientificName, String token) throws BusinessException;
}
