/*
 * Copyright (c) 2017 by Stefan Schubert
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

    /**
     * Picks the aquarium of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the aquarium.
     * @param pPersistedTankId
     * @param pUserId
     * @return null if the aquarium does not belong to the user, or does not exists.
     */
    AquariumEntity getUsersAquarium(Long pPersistedTankId, Long pUserId);

}
