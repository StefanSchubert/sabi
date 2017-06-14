/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.FishEntity;
import org.springframework.stereotype.Repository;

/**
 * Specialized DAO Methods of Aquarium, which are not provided through the standard CRUD impl.
 *
 * @author Stefan Schubert
 */
@Repository("fishDao")
public class FishDaoImpl extends GenericDaoImpl<FishEntity> implements FishDao {


}
