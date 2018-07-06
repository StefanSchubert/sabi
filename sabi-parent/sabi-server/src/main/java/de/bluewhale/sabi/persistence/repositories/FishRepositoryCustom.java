/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishEntity;

/**
 * NOTICE: Though unecessary I leave this as a sample on how to extend spring repositories if required.
 *
 * @author Stefan Schubert
 */
public interface FishRepositoryCustom {

    /**
     * Returns the fish, only if it exists and the user is the Owner.
     * @param pFishId
     * @param pRegisteredUserId
     * @return
     */
    FishEntity findUsersFish(Long pFishId, Long pRegisteredUserId);

}
