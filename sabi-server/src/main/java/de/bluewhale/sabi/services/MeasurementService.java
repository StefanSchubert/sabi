/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UnitTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides all required for dealing with measurements here e.g. for use cases around the {@link de.bluewhale.sabi.persistence.model.MeasurementEntity}
 *
 * @author Stefan Schubert
 */
public interface MeasurementService {

    /**
     * Lists measurements of a specific tank.
     *
     * @param pTankID identifies the tank for which the the measurement were taken.
     * @return List of measurements, maybe empty but never null.
     */
    @NotNull
    List<MeasurementTo> listMeasurements(Long pTankID);

    /**
     * Measurement units are not hard coded via enums, they can dynamically added through the database if required.
     * Use this function to retrieve the list of known ones.
     * @return List of supported measurement units
     */
    @NotNull
    List<UnitTo> listAllMeasurementUnits();

    /**
     * Lists all measurements of a specific user.
     *
     * @param pUserEmail identifies the user who has taken the measurements.
     * @param resultLimit used to limit the number of retrieved records (will return most recent ones). 0 means fetch them all.
     * @return List of measurements, maybe empty but never null.
     */
    @NotNull
    List<MeasurementTo> listMeasurements(@NotNull String pUserEmail, Integer resultLimit);

    /**
     * Removes a measurements (physically)
     * @param pMeasurementID
     * @param pUserEmail
     * @return Composed result object containing the deleted measurement with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO} other possible messages are  {@link Message.CATEGORY#ERROR}
     * Possible reasons:
     * <ul>
     *     <li>{@link TankMessageCodes#UNKNOWN_USER}</li>
     *     <li>{@link TankMessageCodes#MEASURMENT_ALREADY_DELETED}</li>
     * </ul>
     * In case a user requests the deletion of a measurement, that he or she does not own, the service response with an error result
     * message "MEASURMENT_ALREADY_DELETED" disregarding of the root cause (idempotent, fraud...)
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> removeMeasurement(@NotNull Long pMeasurementID, @NotNull String pUserEmail);

    /**
     * Creates a new measurement for provided users tank.
     * @param pMeasurementTo measurement data.
     * @param pUserEmail Owner of the tank for which the measurement has been taken
     * @return Composed result object containing the created measurement with a message. The measurement has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> addMeasurement(@NotNull MeasurementTo pMeasurementTo, @NotNull String pUserEmail);

    /**
     * Updates already taken measurement
     * @param pMeasurementTo
     * @param pUserEmail
     * @return Composed result object containing the updated measurement with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> updateMeasurement(@NotNull MeasurementTo pMeasurementTo, @NotNull String pUserEmail);



}
