/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.LocalizedFishRoleEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for localized fish role descriptions.
 */
public interface LocalizedFishRoleRepository extends CrudRepository<LocalizedFishRoleEntity, Long> {

    List<LocalizedFishRoleEntity> findByLanguageCode(String languageCode);

}
