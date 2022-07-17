/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.model.ResultTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides all required for dealing with Plague records here e.g. for use cases around the {@link de.bluewhale.sabi.persistence.model.PlagueRecordEntity}
 *
 * @author Stefan Schubert
 */
public interface PlagueCenterService {

    /**
     * Lists recorded plagues of a specific tank.
     *
     * @param pTankID identifies the tank for which the plague has been recorded.
     *
     * @return List of plage recurds, maybe empty but never null.
     */
    @NotNull
    List<PlagueRecordTo> listPlaguesRecordsOf(Long pTankID);

    /**
     * PlagueTypes are not hard coded via enums, they can dynamically add through the database if required.
     * Use this function to retrieve the list of known ones.
     * @param usersLanguage Locale.getLanguage used to retrieve the localized plague name
     * @return List of currently tracked plagues
     */
    @NotNull
    List<PlagueTo> listAllPlagueTypes(String usersLanguage);

    /**
     * PlagueStatus are not hard coded via enums, they can dynamically add through the database if required.
     * Use this function to retrieve the list of known ones.
     * @param usersLanguage Locale.getLanguage used to retrieve the localized plague status name
     * @return List of plague status range
     */
    @NotNull
    List<PlagueStatusTo> listAllPlagueStatus(String usersLanguage);

    /**
     * Lists all PlagueRecords of a specific user.
     *
     * @param pUserEmail identifies the user who has taken the measurements.
     * @param resultLimit used to limit the number of retrieved records (will return most recent ones). 0 means fetch them all.
     * @return List of PlagueRecords, maybe empty but never null.
     */
    @NotNull
    List<PlagueRecordTo> listPlagueRecordsOf(@NotNull String pUserEmail, Integer resultLimit);

    /**
     * Removes a PlagueRecord (physically)
     * @param pPlagueRecordID
     * @param pUserEmail
     * @return Composed result object containing the deleted PlagueRecord with a message. The Plague Records  have been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO} other possible messages are  {@link Message.CATEGORY#ERROR}
     * Possible reasons:
     * <ul>
     *     <li>{@link PlagueCenterMessageCodes#UNKNOWN_USER}</li>
     *     <li>{@link PlagueCenterMessageCodes#RECORD_ALREADY_DELETED}</li>
     * </ul>
     * In case a user requests the deletion of a measurement, that he or she does not own, the service response with an error result
     * message "MEASUREMENT_ALREADY_DELETED" disregarding of the root cause (idempotent, fraud...)
     */
    @NotNull
    @Transactional
    ResultTo<PlagueRecordTo> removePlagueRecord(@NotNull Long pPlagueRecordID, @NotNull String pUserEmail);

    /**
     * Creates a new PlagueRecord for provided users tank.
     * @param pPlagueRecordTo measurement data.
     * @param pUserEmail Owner of the tank for which the measurement has been taken
     * @return Composed result object containing the created measurement with a message. The measurement has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<PlagueRecordTo> addPlagueRecord(@NotNull PlagueRecordTo pPlagueRecordTo, @NotNull String pUserEmail);

    /**
     * Updates already taken PlagueRecord
     * @param pPlagueRecordTo
     * @param pUserEmail
     * @return Composed result object containing the updated measurement with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<PlagueRecordTo> updatePlagueRecord(@NotNull PlagueRecordTo pPlagueRecordTo, @NotNull String pUserEmail);

    /**
     * Used to display some project stats.
     * @return Number of overall plague records taken.
     */
    String fetchAmountOfPlagueRecords();
}
