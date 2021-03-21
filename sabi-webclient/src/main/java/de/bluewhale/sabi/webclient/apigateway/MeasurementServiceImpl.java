/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.MeasurementTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Calls Sabi Backend to manage users measurements.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class MeasurementServiceImpl implements MeasurementService {
    @Override
    public @NotNull List<MeasurementTo> getMeasurmentsTakenByUser(@NotNull String JWTBackendAuthtoken, @NotNull Integer maxResultCount) throws BusinessException {
        throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.MeasurementTo> getMeasurmentsTakenByUser([JWTBackendAuthtoken, maxResultCount])");
    }

    @Override
    public @NotNull List<MeasurementTo> getMeasurmentsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException {
        throw new UnsupportedOperationException("java.util.List<de.bluewhale.sabi.model.MeasurementTo> getMeasurmentsForUsersTank([JWTAuthtoken, tankId])");
    }

    @Override
    public void deleteMeasurementById(@NotNull Long measurementId, @NotNull String JWTBackendAuthtoken) throws BusinessException {
        throw new UnsupportedOperationException("void deleteMeasurementById([measurementId, JWTBackendAuthtoken])");
    }

    @Override
    public void save(MeasurementTo measurement, String JWTBackendAuthtoken) throws BusinessException {
        throw new UnsupportedOperationException("void save([measurement, JWTBackendAuthtoken])");
    }
}
