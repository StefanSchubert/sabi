/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.MeasurementTo;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Calls Sabi Backend to retrieve the list of users tank measurements.
 *
 * @author Stefan Schubert
 */
public class MeasurementServiceImpl implements MeasurementService {
    @Override
    public @NotNull List<MeasurementTo> getMeasurmentsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException {
        // FIXME STS (21.03.21): Implementation required
        return Collections.emptyList();
    }
}
