/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 * Author: Stefan Schubert
 */
@Transactional
public interface AquariumDao extends GenericDao<AquariumEntity> {

    /**
     * Used to get an overview of users tanks.
     * @param pUserId OwnerID of the Tanks
     * @return List of Aquariums, that belong to the User.
     */
    @NotNull
    List<AquariumTo> findUsersTanks(@NotNull Long pUserId);

}
