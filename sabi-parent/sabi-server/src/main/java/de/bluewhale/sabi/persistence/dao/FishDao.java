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



}
