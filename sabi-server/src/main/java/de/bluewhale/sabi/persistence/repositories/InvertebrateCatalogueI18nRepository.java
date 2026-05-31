/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueI18nEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for invertebrate catalogue i18n entries.
 * Part of 006-invertebrate-tracking.
 */
public interface InvertebrateCatalogueI18nRepository extends JpaRepository<InvertebrateCatalogueI18nEntity, Long> {
    Optional<InvertebrateCatalogueI18nEntity> findByCatalogueIdAndLanguageCode(Long catalogueId, String languageCode);
}
