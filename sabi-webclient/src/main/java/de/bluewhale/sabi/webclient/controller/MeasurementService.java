/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.MeasurementTo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Interface between Measurements in WebClient and Sabi-Server.
 * Implementation should be stateless, to keep the required resources on the PIs as low as possible.
 * That why the JWTAuthToken, is required to authenticate against backend services.
 *
 * @author Stefan Schubert
 */
public interface MeasurementService extends Serializable {

    /**
     * List Users tanks. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param tankId Id of users tank to which the measures belong.
     * @return List of measurements that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
   @NotNull List<MeasurementTo> getMeasurmentsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException;

}
