/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.MotdEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
@Slf4j
public class MotdRepositoryCustomImpl implements MotdRepositoryCustom {

    @PersistenceContext(unitName = "sabi")
    protected EntityManager em;

    @Override
    public MotdEntity findValidMotd() {

        Query query = em.createNamedQuery("Motd.getValidModt");
        MotdEntity modt = null;
        try {
            modt = (MotdEntity) query.getSingleResult();
        } catch (Exception e) {
            log.error("Please check motd records or query impl. {}",e.getMessage());
            // we handle this as no motd to be resilient.
        }

        return modt;
    }
}
