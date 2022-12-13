/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
     *
     * @return List of supported measurement units
     */
    @NotNull
    List<UnitTo> listAllMeasurementUnits();

    /**
     * Lists all measurements of a specific user.
     *
     * @param pUserEmail  identifies the user who has taken the measurements.
     * @param resultLimit used to limit the number of retrieved records (will return most recent ones). 0 means fetch them all.
     * @return List of measurements, maybe empty but never null.
     */
    @NotNull
    List<MeasurementTo> listMeasurements(@NotNull String pUserEmail, Integer resultLimit);

    /**
     * Removes a measurements (physically)
     *
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
     * message "MEASUREMENT_ALREADY_DELETED" disregarding of the root cause (idempotent, fraud...)
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> removeMeasurement(@NotNull Long pMeasurementID, @NotNull String pUserEmail);

    /**
     * Creates a new measurement for provided users tank.
     *
     * @param pMeasurementTo measurement data.
     * @param pUserEmail     Owner of the tank for which the measurement has been taken
     * @return Composed result object containing the created measurement with a message. The measurement has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> addMeasurement(@NotNull MeasurementTo pMeasurementTo, @NotNull String pUserEmail);

    /**
     * Updates already taken measurement
     *
     * @param pMeasurementTo
     * @param pUserEmail
     * @return Composed result object containing the updated measurement with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<MeasurementTo> updateMeasurement(@NotNull MeasurementTo pMeasurementTo, @NotNull String pUserEmail);


    /**
     * Lists measurements of a specific tank and measured unit.
     *
     * @param pTankID identifies the tank for which the measurement were taken.
     * @param pUnitID identifies the unit for which the measurement were taken.
     * @return List of measurements, maybe empty but never null. Result will be sorted asc according measure date.
     */
    @NotNull
    List<MeasurementTo> listMeasurementsFilteredBy(Long pTankID, Integer pUnitID);

    /**
     * Used to see how long the last measurement for a given unit has been past.
     *
     * @param pTankID the tank to lookup
     * @param pUnitID the unit of concern
     * @return DateTime of last recorded item or null if we haven't found one.
     */
    @Transactional
    LocalDateTime getLastTimeOfMeasurementTakenFilteredBy(Long pTankID, Integer pUnitID);

    /**
     * Provides additional information for requested unit.
     * Required because of the loose coupling.
     *
     * @param pUnitID identifies the unit for which we are interested on additional parameter infos.
     * @return ParameterTo with details or null if they do not exists.
     */
    ParameterTo fetchParameterInfoFor(Integer pUnitID);

    /**
     * Used to display some project stats.
     *
     * @return Number of overall Measurements taken.
     */
    String fetchAmountOfMeasurements();

    /**
     * Creates a new measurement for provided users tank. MUST only be used, if the caller has been authorized beforehand, e.g. via apiKey
     * to add measurements for provided tank.
     *
     * @param pMeasurementTo measurement data.
     * @param pMeasurementTo
     * @return Composed result object containing the created measurement with a message. The measurement has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     * @return
     */
    ResultTo<MeasurementTo> addIotAuthorizedMeasurement(MeasurementTo pMeasurementTo);

    /**
     * The answer combines users profile setting about measurement reminders with the last recently measurement
     * on each reminder setting, with the purpose of calculation of the next required measurement.
     *
     * @param pUserEmail preAuthed UserID
     * @return List of measurement reminders, might be empty.
     */
    @NotNull List<MeasurementReminderTo> fetchUsersNextMeasurements(@NotNull String pUserEmail);

    /**
     * Adds provided MeasurementReminderTo if it is consistent with requesting user.
     *
     * @param reminderTo MeasurementReminderTo which shall be added.
     * @param pUserEmail preAuthed UserID
     * @return ResultTo with the record and a process state.
     * result state can be of <ul>
     * <li>{@link UserSpecificMessageCodes#RECORD_ALREADY_EXISTS}</li>
     * <li>{@link UserSpecificMessageCodes#CREATE_SUCCEEDED}</li>
     * </ul>
     */
    @NotNull ResultTo<MeasurementReminderTo> addMeasurementReminder(@NotNull MeasurementReminderTo reminderTo, @NotNull String pUserEmail);

    /**
     * Updates provided MeasurementReminderTo if it is consistent with requesting user.
     *
     * @param reminderTo MeasurementReminderTo which shall be updated.
     * @param pUserEmail preAuthed UserID
     * @return ResultTo with the record and a process state.
     * result state can be of <ul>
     * <li>{@link UserSpecificMessageCodes#NOT_YOUR_RECORD}</li>
     * <li>{@link UserSpecificMessageCodes#UNKOWN_RECORD}</li>
     * <li>{@link UserSpecificMessageCodes#UPDATE_SUCCEEDED}</li>
     * </ul>
     */
    @NotNull ResultTo<MeasurementReminderTo> updateMeasurementReminder(@NotNull MeasurementReminderTo reminderTo, @NotNull String pUserEmail);

    /**
     * Delete provided MeasurementReminderTo if it is consistent with requesting user.
     *
     * @param reminderTo MeasurementReminderTo which shall be updated.
     * @param pUserEmail preAuthed UserID
     * @return ResultTo with the record and a process state.
     * result state can be of <ul>
     * <li>{@link UserSpecificMessageCodes#NOT_YOUR_RECORD}</li>
     * <li>{@link UserSpecificMessageCodes#UNKOWN_RECORD}</li>
     * <li>{@link UserSpecificMessageCodes#REMOVAL_SUCCEEDED}</li>
     * </ul>
     */
    @NotNull ResultTo<MeasurementReminderTo> deleteMeasurementReminder(@NotNull MeasurementReminderTo reminderTo, @NotNull String pUserEmail);

}
