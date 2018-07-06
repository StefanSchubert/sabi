/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface MeasurementRepository extends JpaRepository<MeasurementEntity, Long> {

    /**
     * Used to get an overview of users measurements.
     * @param pUserId OwnerID of the measurements
     * @return List of Measurements, that belong to the User.
     */
    @NotNull
   // List<MeasurementTo> findUsersMeasurements(@NotNull Long pUserId);

    List<MeasurementEntity> findMeasurementEntitiesByUser(@NotNull UserEntity user);
    List<MeasurementEntity> findMeasurementEntitiesByUser_IdIs(@NotNull Long userId);

    /**
     * Retrieves a measurement of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the measurement.
     * @param pPersistedMeasurementId
     * @param pUserId
     * @return null if the measurement does not belong to the user, or does not exists.
     */
    // MeasurementTo getUsersMeasurement(@NotNull Long pPersistedMeasurementId, @NotNull Long pUserId);

    MeasurementEntity getMeasurementEntityByIdAndUser(@NotNull Long pPersistedMeasurementId, @NotNull UserEntity user);
    MeasurementEntity getMeasurementEntityByIdAndUser_IdIs(@NotNull Long pPersistedMeasurementId, @NotNull Long userId);

    /**
     * Retrieves all measurements for a specific tank.
     * @param pTankID identifies your aquarium.
     * @return empty list, if no measurement have been found.
     */
    @NotNull
    // List<MeasurementTo> listTanksMeasurements(Long pTankID);

    List<MeasurementEntity> findMeasurementEntitiesByAquarium(@NotNull AquariumEntity aquariumEntity);
    List<MeasurementEntity> findMeasurementEntitiesByAquarium_IdIs(@NotNull Long aquariumId);

}
