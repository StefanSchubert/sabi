/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.ParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.validation.constraints.NotNull;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface ParameterRepository extends JpaRepository<ParameterEntity, Long> {

    /**
     * Unit Parameter lookup. Used to fetch details Info of a measurement unit, mainly threshold values.
     * @param pUnitId
     * @return null if no detail Info available, or belonging Parameter infos.
     */
    ParameterEntity findByBelongingUnitIdEquals(@NotNull Integer pUnitId);

}
