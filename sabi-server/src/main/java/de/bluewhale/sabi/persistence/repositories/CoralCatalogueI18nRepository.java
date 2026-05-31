/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.CoralCatalogueI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for CoralCatalogueI18nEntity.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public interface CoralCatalogueI18nRepository extends JpaRepository<CoralCatalogueI18nEntity, Long> {

    /** Find the localised fields for a specific language and catalogue entry. */
    Optional<CoralCatalogueI18nEntity> findByCatalogueIdAndLanguageCode(Long catalogueId, String languageCode);
}

