/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface FishRepository extends CrudRepository<FishEntity, Long>, FishRepositoryCustom {

    /**
     * Fetech all "fishful" ;-) inhabitants of a certain aquarium.
     * @param aquariumId
     * @return
     */
    List<FishEntity> findFishEntitiesByAquariumId(Long aquariumId);

    // FishEntity findFishEntityByIdAndUser

}
