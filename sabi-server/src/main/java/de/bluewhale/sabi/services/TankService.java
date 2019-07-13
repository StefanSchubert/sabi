/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Provides all required services for use cases around the {@link de.bluewhale.sabi.persistence.model.AquariumEntity}
 */
public interface TankService {

    /**
     * Creates a new tank for provided user.
     * @param pAquariumTo Tank data.
     * @param pRegisteredUsersEmail Owner of the tank
     * @return Composed result object containing the created tank with a message. The tank has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<AquariumTo> registerNewTank(@NotNull AquariumTo pAquariumTo, @NotNull String pRegisteredUsersEmail);


    /**
     * Lists tanks for a specific user.
     * @param pUserEmail identifies the owner of the tanks that will be returned.
     * @return List of tanks, maybe empty but never null.
     */
    @NotNull
    List<AquariumTo> listTanks(@NotNull String pUserEmail);

    /**
     * Updates some Tank-Properties
     * @param aquariumTo
     * @param pUsersEmail
     * @return Composed result object containing the updated tank with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<AquariumTo> updateTank(AquariumTo aquariumTo, String pUsersEmail);

    /**
     * Retrieves the requested Tank of provided user
     * @param aquariumId
     * @param pUsersEmail
     * @return Null if tank is not within given users tank list.
     */
    @NotNull
    AquariumTo getTank(Long aquariumId, String pUsersEmail);

    /**
     * Removes a tank from Users List (removes it physically)
     * @param persistedTankId
     * @param pUsersEmail
     * @return Composed result object containing the deleted tank with a message. The tank has been removed successfully
     * only if the message is of {@link Message.CATEGORY#INFO} other possible messages are  {@link Message.CATEGORY#ERROR}
     * Possible reasons:
     * <ul>
     *     <li>{@link TankMessageCodes#NOT_YOUR_TANK}</li>
     *     <li>{@link TankMessageCodes#UNKNOWN_USER}</li>
     * </ul>
     */
    @Transactional
    ResultTo<AquariumTo> removeTank(Long persistedTankId, String pUsersEmail);
}
