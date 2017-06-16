/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.FishEntity;
import org.springframework.transaction.annotation.Transactional;


/**
 *
 * Author: Stefan Schubert
 */
@Transactional
public interface FishDao extends GenericDao<FishEntity> {


    /**
     * Returns the fish, only if it exists and the user is the Owner.
     * @param pFishId
     * @param pRegisteredUserId
     * @return
     */
    FishEntity findUsersFish(Long pFishId, Long pRegisteredUserId);
}
