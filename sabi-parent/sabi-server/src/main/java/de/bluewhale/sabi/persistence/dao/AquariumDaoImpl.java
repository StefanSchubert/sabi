/*
 * Copyright (c) 2016. by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import org.springframework.stereotype.Repository;

/**
 * Specialized DAO Methods of Aquarium, which are not provided through the standard CRUD impl.
 *
 * @author Stefan Schubert
 */
@Repository("aquariumDao")
public class AquariumDaoImpl extends GenericDaoImpl<AquariumEntity> implements AquariumDao {


}
