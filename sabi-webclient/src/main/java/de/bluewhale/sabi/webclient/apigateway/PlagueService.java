/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.List;

/**
 * Interface between PlagueRecord-Data in WebClient and Sabi-Server.
 * Implementation should be stateless, to keep the required resources on the PIs as low as possible.
 * That why the JWTAuthToken, is required to authenticate against backend services.
 *
 * @author Stefan Schubert
 */
public interface PlagueService extends Serializable {

    /**
     * To avoid unnecessary Backend calls, the implementation is suggested to
     * cache the results.
     * <p>
     * TODO: JMX Beans such that the cache can be reloaded in case
     * the backend introduces more units.
     *
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @return List of plagues known by the backend.
     * @throws BusinessException in case of backend auth failures.
     */
    @Cacheable
    @NotNull List<PlagueTo> getPlagueCatalogue(@NotNull String JWTBackendAuthtoken) throws BusinessException;

    /**
     * List Users PlagueRecords for a specific tank. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param tankId       Id of users tank to which the measures belong.
     * @return List of PlagueRecords that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
    @NotNull List<PlagueRecordTo> getPlagueRecordsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException;

    /**
     * List Users PlagueRecords for a specific tank and plague unit. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param tankId       Id of users tank to which the measures belong.
     * @param plagueId     Id which is used to filter the results for a specific plagueId.
     * @return List of PlagueRecords that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
    @NotNull List<PlagueRecordTo> getPlagueRecordsForUsersTankFilteredByUnit(@NotNull String JWTAuthtoken, @NotNull Long tankId, @NotNull Integer plagueId) throws BusinessException;

    /**
     * Request PlagueRecords deletion in Backend, in case he or she did a typo.
     *
     * @param plagueRecordId      Identifier of the Measurement to delete
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @throws BusinessException
     */
    void deletePlagueRecordById(@NotNull Long plagueRecordId, @NotNull String JWTBackendAuthtoken) throws BusinessException;

    /**
     * Update an existing or create a plague record entry for the user/tank.
     *
     * @param plagueRecord        PlagueRecordTo Entry to patch or to create
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @throws BusinessException
     */
    void save(PlagueRecordTo plagueRecord, @NotNull String JWTBackendAuthtoken) throws BusinessException;

}
