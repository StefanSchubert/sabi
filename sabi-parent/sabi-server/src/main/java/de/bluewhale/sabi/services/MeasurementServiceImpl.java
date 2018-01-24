/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides all required services for use cases around the {@link de.bluewhale.sabi.persistence.model.MeasurementEntity}
 *
 * @author Stefan Schubert
 */
@Service
public class MeasurementServiceImpl extends CommonService implements MeasurementService {

    @Override
    public List<MeasurementTo> listMeasurements(Long pTankID) {
        return null;
    }

    @Override
    public List<MeasurementTo> listMeasurements(String pUserEmail) {
        return null;
    }

    @Override
    public ResultTo<MeasurementTo> removeMeasurement(Long pMeasurementID, String pUserEmail) {
        return null;
    }

    @Override
    public ResultTo<MeasurementTo> addMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {
        return null;
    }

    @Override
    public ResultTo<MeasurementTo> updateMeasurement(MeasurementTo pMeasurementTo, String pUserEmail) {
        return null;
    }

}
