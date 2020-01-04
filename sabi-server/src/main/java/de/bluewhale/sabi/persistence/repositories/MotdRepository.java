/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.MotdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface MotdRepository extends JpaRepository<MotdEntity, Long>, MotdRepositoryCustom {

// default repository

}
