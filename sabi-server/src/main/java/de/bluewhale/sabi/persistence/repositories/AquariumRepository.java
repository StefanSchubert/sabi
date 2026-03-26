/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

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
    default List<AquariumEntity> findAllByUser_IdIs(@NotNull Long pUserId) {
        // Default: only active aquariums
        return findAllByUser_IdIsAndActiveIs(pUserId, true);
    }

    /**
     * Same as {@link #findAllByUser_IdIs(Long)} but allows to filter by the entity's "active" flag.
     * Implemented by Spring Data (derived query).
     */
    @NotNull
    List<AquariumEntity> findAllByUser_IdIsAndActiveIs(@NotNull Long pUserId, boolean active);

    /**
     * Picks the aquarium of provided user.
     * The underlying query ensured, that the user (in second parameter) is indeed the owner of the aquarium.
     * @param pPersistedTankId ID of the aquarium to fetch
     * @param pUserId ID of the user (owner) to verify ownership
     * @return null if the aquarium does not belong to the user, or does not exists.
     */
     AquariumEntity getAquariumEntityByIdAndUser_IdIs(Long pPersistedTankId, Long pUserId);


    /**
     * Fetches the aquarium with provided temperature APIKey
     * @param apiKey for temperature submission through IoT devices
     * @return null if we found no match, or the tank belonging to the api key.
     */
     AquariumEntity getAquariumEntityByTemperatureApiKeyEquals(String apiKey);

    /**
     * @return Amount of tanks for which the User has activated the API Key Feature.
     */
    Long countAquariumEntitiesByTemperatureApiKeyNotNull();
}
