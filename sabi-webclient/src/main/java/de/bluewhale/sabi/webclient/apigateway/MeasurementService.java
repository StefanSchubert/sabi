/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UnitTo;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Interface between Measurement-Data in WebClient and Sabi-Server.
 * Implementation should be stateless, to keep the required resources on the PIs as low as possible.
 * That why the JWTAuthToken, is required to authenticate against backend services.
 *
 * @author Stefan Schubert
 */
public interface MeasurementService extends Serializable {

    /**
     * List Users tanks. Concrete user will be derived by the calling context
     *
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param maxResultCount If a user has 100 measurements, It won't make sense to retrieve them all,
     *                       in case we want to display only some latest ones in the view. So we can
     *                       use this param to limit the results, which will be the youngest entries.
     *                       <b>A maxResultCount of 0 means retrieves them all.</b>
     * @return List of measurements that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
   @NotNull List<MeasurementTo> getMeasurementsTakenByUser(@NotNull String JWTBackendAuthtoken, @NotNull Integer maxResultCount) throws BusinessException;

    /**
     * To avoid unnecessary Backend calls, the implementation is suggested to
     * cache the results.
     *
     * TODO: JMX Beans such that the cache can be reloaded in case
     * the backend introduces more units.
     *
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @return List of units known by the backend.
     * @throws BusinessException in case of backend auth failures.
     */
   @NotNull List<UnitTo> getAvailableMeasurementUnits(@NotNull String JWTBackendAuthtoken) throws BusinessException;

    /**
     * List Users Measurements for a specific tank. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param tankId Id of users tank to which the measures belong.
     * @return List of measurements that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
    @NotNull List<MeasurementTo> getMeasurementsForUsersTank(@NotNull String JWTAuthtoken, @NotNull Long tankId) throws BusinessException;


    /**
     * List Users Measurements for a specific tank and measurement unit. Concrete user will be derived by the calling context
     *
     * @param JWTAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @param tankId Id of users tank to which the measures belong.
     * @param unitId Id which is used to filter the results for a specifc measurement unit.
     * @return List of measurements that belong to current user. List may be empty but never NULL.
     * @throws BusinessException in case of backend auth failures.
     */
    @NotNull List<MeasurementTo> getMeasurementsForUsersTankFilteredByUnit(@NotNull String JWTAuthtoken, @NotNull Long tankId, @NotNull Integer unitId) throws BusinessException;

    /**
     * Request Measurement deletion in Backend, in case he or she did a typo.
     * @param measurementId Identifier of the Measurement to delete
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @throws BusinessException
     */
    void deleteMeasurementById(@NotNull Long measurementId, @NotNull String JWTBackendAuthtoken) throws BusinessException;

    /**
     * Update an existing or create a measurement entry for the user.
     * @param measurement Measurement Entry to patch or to create
     * @param JWTBackendAuthtoken Bearer Auth string, which identifies the user against the backend.
     * @throws BusinessException
     */
    void save(MeasurementTo measurement, String JWTBackendAuthtoken) throws BusinessException;
}
