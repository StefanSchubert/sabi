/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface AquariumRepository extends JpaRepository<AquariumEntity, Long> {

    /**
     * Used to get an overview of users tanks.
     * @param pUserId OwnerID of the Tanks
     * @return List of Aquariums, that belong to the User.
     */
    @NotNull
    List<AquariumEntity> findAllByUser_IdIs(@NotNull Long pUserId);

    /**
     * Picks the aquarium of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the aquarium.
     * @param pPersistedTankId
     * @param pUserId
     * @return null if the aquarium does not belong to the user, or does not exists.
     */
     AquariumEntity getAquariumEntityByIdAndUser_IdIs(Long pPersistedTankId, Long pUserId);
}
