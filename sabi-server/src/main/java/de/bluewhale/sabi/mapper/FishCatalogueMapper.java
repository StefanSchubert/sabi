/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueI18nTo;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.FishCatalogueStatus;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * MapStruct mapper for FishCatalogueEntryEntity <-> FishCatalogueEntryTo.
 * Includes language-aware search result mapping with i18n fallback.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Mapper(componentModel = "spring")
public interface FishCatalogueMapper {

    @Mappings({
            @Mapping(target = "proposer", ignore = true),
            @Mapping(target = "status", expression = "java(to.getStatus() != null ? to.getStatus().name() : \"PUBLIC\")")
    })
    FishCatalogueEntryEntity mapTo2Entity(FishCatalogueEntryTo to);

    @Mappings({
            @Mapping(target = "status", expression = "java(entity.getStatus() != null ? de.bluewhale.sabi.model.FishCatalogueStatus.valueOf(entity.getStatus()) : null)")
    })
    FishCatalogueEntryTo mapEntity2To(FishCatalogueEntryEntity entity);

    FishCatalogueI18nEntity mapI18nTo2Entity(FishCatalogueI18nTo to);

    FishCatalogueI18nTo mapI18nEntity2To(FishCatalogueI18nEntity entity);

    @Mappings({
            @Mapping(target = "commonName", expression = "java(resolveI18nName(entity, lang))"),
            @Mapping(target = "referenceUrl", expression = "java(resolveI18nUrl(entity, lang))"),
            @Mapping(target = "status", expression = "java(entity.getStatus() != null ? de.bluewhale.sabi.model.FishCatalogueStatus.valueOf(entity.getStatus()) : null)")
    })
    FishCatalogueSearchResultTo mapEntity2SearchResult(FishCatalogueEntryEntity entity, @Context String lang);

    /**
     * Resolve common name with language fallback: requested lang -> en -> first available.
     */
    default String resolveI18nName(FishCatalogueEntryEntity entity, String lang) {
        if (entity.getI18nEntries() == null || entity.getI18nEntries().isEmpty()) {
            return null;
        }
        // Try requested language
        return entity.getI18nEntries().stream()
                .filter(i -> lang.equals(i.getLanguageCode()) && i.getCommonName() != null)
                .map(FishCatalogueI18nEntity::getCommonName)
                .findFirst()
                // Fallback to EN
                .orElseGet(() -> entity.getI18nEntries().stream()
                        .filter(i -> "en".equals(i.getLanguageCode()) && i.getCommonName() != null)
                        .map(FishCatalogueI18nEntity::getCommonName)
                        .findFirst()
                        // Fallback to first available
                        .orElseGet(() -> entity.getI18nEntries().stream()
                                .map(FishCatalogueI18nEntity::getCommonName)
                                .filter(n -> n != null)
                                .findFirst()
                                .orElse(null)));
    }

    /**
     * Resolve reference URL with language fallback: requested lang -> en -> first available.
     */
    default String resolveI18nUrl(FishCatalogueEntryEntity entity, String lang) {
        if (entity.getI18nEntries() == null || entity.getI18nEntries().isEmpty()) {
            return null;
        }
        return entity.getI18nEntries().stream()
                .filter(i -> lang.equals(i.getLanguageCode()) && i.getReferenceUrl() != null)
                .map(FishCatalogueI18nEntity::getReferenceUrl)
                .findFirst()
                .orElseGet(() -> entity.getI18nEntries().stream()
                        .filter(i -> "en".equals(i.getLanguageCode()) && i.getReferenceUrl() != null)
                        .map(FishCatalogueI18nEntity::getReferenceUrl)
                        .findFirst()
                        .orElseGet(() -> entity.getI18nEntries().stream()
                                .map(FishCatalogueI18nEntity::getReferenceUrl)
                                .filter(u -> u != null)
                                .findFirst()
                                .orElse(null)));
    }
}
