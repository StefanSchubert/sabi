/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 * Provides all required controller for use cases around the {@link de.bluewhale.sabi.persistence.model.FishEntity}
 */
public interface FishService {


    /**
     * Adds a new fish to the tank of the user
     * @param pFishTo
     * @param pRegisteredUser
     * @return Composed result object containing the created fish with a message. The fish has been added successfully
     * only if the message is of {@link Message.CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<FishTo> registerNewFish(@NotNull FishTo pFishTo, @NotNull UserTo pRegisteredUser);

    /**
     * Removes a fish from users records (e.g. tank)
     * @param pFishId
     * @param pRegisteredUser
     */
    @Transactional
    void removeFish(@NotNull Long pFishId, @NotNull UserTo pRegisteredUser);

    /**
     * Retrieves a users fish, as long as he or she is the owner
     * @param pFishId
     * @param registeredUser
     * @return null if the fish does not exists or if the user is not the owner.
     */
    FishTo getUsersFish(@NotNull Long pFishId, @NotNull UserTo registeredUser);
}
