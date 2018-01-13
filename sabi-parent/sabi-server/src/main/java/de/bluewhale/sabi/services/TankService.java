/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
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
     * @param pRegisteredUser Owner of the tank (email address)
     * @return Composed result object containing the created tank with a message. The tank has been created successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<AquariumTo> registerNewTank(@NotNull AquariumTo pAquariumTo, @NotNull String pRegisteredUser);


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
     * @param pUser (email-address)
     * @return Composed result object containing the updated tank with a message. The tank has been updated successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<AquariumTo> updateTank(AquariumTo aquariumTo, String pUser);

    /**
     * Retrieves the requested Tank of provided user
     * @param aquariumId
     * @param registeredUser (Email-Address)
     * @return Null if tank is not within given users tank list.
     */
    @NotNull
    AquariumTo getTank(Long aquariumId, String registeredUser);

    /**
     * Removes a tank from Users List (removes it physically)
     * @param persistedTankId
     * @param registeredUser
     */
    @Transactional
    void removeTank(Long persistedTankId, UserTo registeredUser);
}
