/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Interface between Tankdata in WebClient and Sabi-Server.
 * Implementation should be stateless, to keep the required resources on the PIs as low as possible.
 * That why the JWTAuthToken, is required to authenticate against backend services.
 *
 * @author Stefan Schubert
 */
public interface TankService extends Serializable {

    /**
     * List Users tanks. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @return List of tanks that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
   @NotNull List<AquariumTo> getUsersTanks(@NotNull String JWTAuthtoken) throws BusinessException;

}
