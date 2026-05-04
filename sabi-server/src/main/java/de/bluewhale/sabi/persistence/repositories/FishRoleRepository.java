/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishRoleEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for fish role master data.
 */
public interface FishRoleRepository extends CrudRepository<FishRoleEntity, Integer> {

    List<FishRoleEntity> findAll();

}
