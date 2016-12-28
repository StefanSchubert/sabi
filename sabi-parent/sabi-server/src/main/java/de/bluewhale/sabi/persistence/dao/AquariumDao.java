/*
 * Copyright (c) 2016. by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * Author: Stefan Schubert
 */
@Transactional
public interface AquariumDao extends GenericDao<AquariumEntity> {



}
