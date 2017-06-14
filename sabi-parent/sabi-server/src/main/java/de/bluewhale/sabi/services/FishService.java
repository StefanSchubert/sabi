/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 * Provides all required services for use cases around the {@link de.bluewhale.sabi.persistence.model.FishEntity}
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
}
