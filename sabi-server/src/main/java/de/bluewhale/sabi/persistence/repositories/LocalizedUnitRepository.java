/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.LocalizedUnitEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SpringDataRepository
 *
 * @author Stefan Schubert
 */
public interface LocalizedUnitRepository extends JpaRepository<LocalizedUnitEntity, Long> {

    /**
     * Used to get the language specific fields.
     * @param pLanguage
     * @param pUnitId
     * @return LocalizedUnitEntity suitable for requested language
     */
    LocalizedUnitEntity findByLanguageAndUnitId(@NotNull String pLanguage, @NotNull Integer pUnitId);


}
