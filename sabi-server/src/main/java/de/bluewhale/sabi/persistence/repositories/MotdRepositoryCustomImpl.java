/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.MotdEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public class MotdRepositoryCustomImpl implements MotdRepositoryCustom {

    static Logger logger = LoggerFactory.getLogger(MotdRepositoryCustomImpl.class);

    @PersistenceContext(unitName = "sabi")
    protected EntityManager em;

    @Override
    public MotdEntity findValidMotd() {

        Query query = em.createNamedQuery("Motd.getValidModt");
        MotdEntity modt = null;
        try {
            modt = (MotdEntity) query.getSingleResult();
        } catch (Exception e) {
            logger.error("Please check motd records or query impl. "+e);
            // we handle this as no motd to be resilient.
        }

        return modt;
    }
}
