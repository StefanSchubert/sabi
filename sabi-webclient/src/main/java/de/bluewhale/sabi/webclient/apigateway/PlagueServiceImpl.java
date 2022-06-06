/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueTo;

import java.util.Collections;
import java.util.List;

/**
 * Calls Sabi Backend to manage users plague records.
 *
 * @author Stefan Schubert
 */
public class PlagueServiceImpl implements PlagueService {
    @Override
    public List<PlagueTo> getPlagueCatalogue(String JWTBackendAuthtoken) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTank(String JWTAuthtoken, Long tankId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public List<PlagueRecordTo> getPlagueRecordsForUsersTankFilteredByUnit(String JWTAuthtoken, Long tankId, Integer plagueId) throws BusinessException {
        return Collections.emptyList();
    }

    @Override
    public void deletePlagueRecordById(Long plagueRecordId, String JWTBackendAuthtoken) throws BusinessException {

    }

    @Override
    public void save(PlagueRecordTo plagueRecord, String JWTBackendAuthtoken) throws BusinessException {

    }
}
