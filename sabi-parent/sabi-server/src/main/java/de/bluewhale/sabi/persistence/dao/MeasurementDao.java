/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 * Author: Stefan Schubert
 */
@Transactional
public interface MeasurementDao extends GenericDao<MeasurementEntity> {

    /**
     * Used to get an overview of users measurements.
     * @param pUserId OwnerID of the measurements
     * @return List of Measurements, that belong to the User.
     */
    @NotNull
    List<MeasurementTo> findUsersMeasurements(@NotNull Long pUserId);

    /**
     * Retrieves a measurement of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the measurement.
     * @param pPersistedMeasurementId
     * @param pUserId
     * @return null if the measurement does not belong to the user, or does not exists.
     */
    MeasurementTo getUsersMeasurement(@NotNull Long pPersistedMeasurementId, @NotNull Long pUserId);

}
