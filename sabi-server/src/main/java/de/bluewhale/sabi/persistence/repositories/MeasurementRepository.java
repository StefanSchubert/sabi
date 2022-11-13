/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

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
     * @param user, i.e. owner of the measurements
     * @return List of Measurements, that belong to the User.
     */
    @NotNull List<MeasurementEntity> findByUserOrderByMeasuredOnDesc(@NotNull UserEntity user);

    /**
     * Used to get an overview of users measurements.
     * @param user, i.e. owner of the measurements
     * @param pageable, defines how many results should be retrieved (pageble) and how to be sorted,
     *                  example <i>Pageable page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "measuredOn"));</i>
     *                  <b>UPDATE:</b> Page sorting doesn't seem to work so we sort directly
     * @return List of Measurements, that belong to the User.
     */
    @NotNull List<MeasurementEntity> findByUserOrderByMeasuredOnDesc(@NotNull UserEntity user, @NotNull Pageable pageable);

    /**
     * Used to get an overview of users measurements.
     * @param userId, identifies the owner of the measurements
     * @return List of Measurements, that belong to the User.
     */
    @NotNull List<MeasurementEntity> findByUser_IdIs(@NotNull Long userId);

    /**
     * Retrieves a measurement of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the measurement.
     * @param pPersistedMeasurementId
     * @param pUserId
     * @return null if the measurement does not belong to the user, or does not exists.
     */
    @Nullable MeasurementEntity getByIdAndUser_Id(@NotNull Long pPersistedMeasurementId, @NotNull Long pUserId);

    @Nullable MeasurementEntity getByIdAndUser(@NotNull Long pPersistedMeasurementId, @NotNull UserEntity user);

    /**
     * Retrieves all measurements for a specific tank.
     * @param aquariumId identifies your aquarium.
     * @return empty list, if no measurement have been found.
     */
    @NotNull List<MeasurementEntity> findByAquarium_Id(@NotNull Long aquariumId);

    /**
     * Retrieves all measurements for a specific tank.
     * @return empty list, if no measurement have been found.
     */
    @NotNull List<MeasurementEntity> findByAquarium(@NotNull AquariumEntity aquariumEntity);

    /**
     * Retrieves all measurements for a specific tank filtered by given Unit ID.
     * @param unitID identifies your unit.
     * @return empty list, if no measurement have been found.
     */
    @NotNull List<MeasurementEntity> findByAquariumAndUnitIdOrderByMeasuredOnAsc(@NotNull AquariumEntity aquariumEntity, @NotNull Integer unitID);

    /**
     * Retrieves last recent measurement of a specific measurement unit for given user
     * @param unitID identifies your unit.
     * @param userEntity search will be limited to
     * @return measurement or null, if no measurement exists yet.
     */
    @Nullable MeasurementEntity findByUserAndUnitIdAndMeasuredOnMax(@NotNull UserEntity userEntity, @NotNull Integer unitID);

    /**
     * Retrieves the latest measurement of provided unit for given tank.
     * @param aquariumId identifies your aquarium.
     * @param unitID identifies the requested unit.
     * @return latest Measurement or null if no such one exists.
     */
    @Nullable MeasurementEntity findTopByAquarium_IdAndUnitIdOrderByMeasuredOnDesc(@NotNull Long aquariumId, @NotNull Integer unitID);

    /**
     * Used to count measurements which does not belong to the test user (if you pass the right user ;-) )
     * @param user to be excluded
     * @return number of measurements without those of given user
     */
    @NotNull Long countAllByUserIsNot(@NotNull UserEntity user);

}
