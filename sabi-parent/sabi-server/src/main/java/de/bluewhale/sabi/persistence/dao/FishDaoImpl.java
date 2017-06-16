/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.FishEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;

/**
 * Specialized DAO Methods of Aquarium, which are not provided through the standard CRUD impl.
 *
 * @author Stefan Schubert
 */
@Repository("fishDao")
public class FishDaoImpl extends GenericDaoImpl<FishEntity> implements FishDao {

    @Override
    public FishEntity findUsersFish(Long pFishId, Long pRegisteredUserId) {
        Query query = em.createNamedQuery("Fish.getUsersFish");
        query.setParameter("pUserId", pRegisteredUserId);
        query.setParameter("pFishId", pFishId);
        FishEntity fish = null;
        try {
            fish = (FishEntity) query.getSingleResult();
        } catch (Exception e) {
            // todo Just log the request;
        }
        return fish;
    }
}
