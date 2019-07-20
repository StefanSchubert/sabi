/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
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
