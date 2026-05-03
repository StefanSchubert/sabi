/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import jakarta.validation.constraints.NotNull;

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
    * @param pJWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
    * @return List of active tanks that belong to current user. List may be empty but never NULL.
    * @throws BusinessException in case of backend auth failures.
    */
   @NotNull List<AquariumTo> getUsersTanks(@NotNull String pJWTBackendAuthtoken) throws BusinessException;

   /**
    * List ALL users tanks including inactive ones. Required for history views like PlagueCenter
    * to resolve tank names for past records of meanwhile deactivated tanks.
    *
    * @param pJWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
    * @return List of all tanks (active + inactive) that belong to current user. List may be empty but never NULL.
    * @throws BusinessException in case of backend auth failures.
    */
   @NotNull List<AquariumTo> getAllUsersTanks(@NotNull String pJWTBackendAuthtoken) throws BusinessException;

    /**
     * Request Tank deletion in Backend.
     * @param tankId Identifier of the tank to delete
     * @param pJWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @throws BusinessException
     */
    void deleteTankById(@NotNull Long tankId, @NotNull String pJWTBackendAuthtoken) throws BusinessException;

    /**
     * Update an existing or create a new tank for the user.
     * @param tank Tank to patch or to create
     * @param pJWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @return the saved tank (with server-assigned ID for new tanks)
     * @throws BusinessException
     */
    AquariumTo save(AquariumTo tank, String pJWTBackendAuthtoken) throws BusinessException;

    /**
     * Retrieves a new temperature API Key.
     * @param tankID Tank on which the API Key will be registered
     * @param pJWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @return generated and stored API Key for submitting temperature measurements
     * @throws BusinessException
     */
    String reCreateTemperatureAPIKey(@NotNull Long tankID, @NotNull String pJWTBackendAuthtoken) throws BusinessException;

    /**
     * Uploads a photo for the given aquarium.
     * @param aquariumId ID of the aquarium
     * @param bytes raw image bytes
     * @param contentType MIME type
     * @param token Bearer auth token
     * @throws BusinessException
     */
    void uploadPhoto(@NotNull Long aquariumId, @NotNull byte[] bytes, @NotNull String contentType, @NotNull String token) throws BusinessException;

    /**
     * Downloads the photo bytes for the given aquarium from backend.
     * @param aquariumId ID of the aquarium
     * @param token Bearer auth token
     * @return raw image bytes, empty array if none
     * @throws BusinessException
     */
    @NotNull byte[] getPhoto(@NotNull Long aquariumId, @NotNull String token) throws BusinessException;
}
