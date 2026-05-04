/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for localized fish catalogue i18n entries.
 *
 * @author Stefan Schubert
 */
public interface FishCatalogueI18nRepository extends JpaRepository<FishCatalogueI18nEntity, Long> {

    Optional<FishCatalogueI18nEntity> findByCatalogueIdAndLanguageCode(Long catalogueId, String languageCode);

}

