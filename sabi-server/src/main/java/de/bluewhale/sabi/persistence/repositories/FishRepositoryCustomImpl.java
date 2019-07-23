/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Repository extension, required for functions that could not be proxied by spring-data directly.
 *
 * @author Stefan Schubert
 */
public class FishRepositoryCustomImpl implements FishRepositoryCustom {

    @PersistenceContext(unitName = "sabi")
    protected EntityManager em;

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
