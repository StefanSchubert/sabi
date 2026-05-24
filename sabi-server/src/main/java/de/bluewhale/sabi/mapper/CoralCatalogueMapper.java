/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.CoralCatalogueEntity;
import de.bluewhale.sabi.persistence.model.CoralCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.TankCoralStockEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mapper for CoralCatalogueEntity &lt;-&gt; CoralCatalogueEntryTo and search results.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Component
public class CoralCatalogueMapper {

    public CoralCatalogueEntryTo toTo(CoralCatalogueEntity entity) {
        if (entity == null) return null;
        CoralCatalogueEntryTo to = new CoralCatalogueEntryTo();
        to.setId(entity.getId());
        to.setScientificName(entity.getScientificName());
        to.setClassification(entity.getClassification() != null
                ? CoralClassification.valueOf(entity.getClassification()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setStatus(entity.getStatus() != null ? FishCatalogueStatus.valueOf(entity.getStatus()) : null);
        to.setProposerUserId(entity.getProposerUserId());
        to.setProposalDate(entity.getProposalDate());

        if (entity.getI18nEntries() != null) {
            List<CoralCatalogueI18nTo> i18nTos = new ArrayList<>();
            entity.getI18nEntries().forEach(i18n -> i18nTos.add(toI18nTo(i18n)));
            to.setI18nEntries(i18nTos);
        }
        return to;
    }

    public CoralCatalogueI18nTo toI18nTo(CoralCatalogueI18nEntity entity) {
        if (entity == null) return null;
        CoralCatalogueI18nTo to = new CoralCatalogueI18nTo();
        to.setId(entity.getId());
        to.setLanguageCode(entity.getLanguageCode());
        to.setCommonName(entity.getCommonName());
        to.setDescription(entity.getDescription());
        to.setReferenceUrl(entity.getReferenceUrl());
        return to;
    }

    public CoralCatalogueEntity toEntity(CoralCatalogueEntryTo to) {
        if (to == null) return null;
        CoralCatalogueEntity entity = new CoralCatalogueEntity();
        entity.setId(to.getId());
        entity.setScientificName(to.getScientificName());
        entity.setClassification(to.getClassification() != null ? to.getClassification().name() : null);
        entity.setCareLevel(to.getCareLevel() != null ? to.getCareLevel().name() : null);
        entity.setStatus(to.getStatus() != null ? to.getStatus().name() : "PENDING");
        entity.setProposerUserId(to.getProposerUserId());
        entity.setProposalDate(to.getProposalDate());
        return entity;
    }

    /**
     * Convert a catalogue entity to a search result, resolving i18n fields for the requested language.
     * Falls back to 'en' if the requested language is not available.
     */
    public CoralCatalogueSearchResultTo toSearchResult(CoralCatalogueEntity entity, String lang) {
        if (entity == null) return null;
        CoralCatalogueSearchResultTo to = new CoralCatalogueSearchResultTo();
        to.setId(entity.getId());
        to.setScientificName(entity.getScientificName());
        to.setClassification(entity.getClassification() != null
                ? CoralClassification.valueOf(entity.getClassification()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setStatus(entity.getStatus() != null ? FishCatalogueStatus.valueOf(entity.getStatus()) : null);

        // Resolve i18n fields
        if (entity.getI18nEntries() != null) {
            CoralCatalogueI18nEntity i18n = entity.getI18nEntries().stream()
                    .filter(e -> lang != null && lang.equals(e.getLanguageCode()))
                    .findFirst()
                    .orElse(entity.getI18nEntries().stream()
                            .filter(e -> "en".equals(e.getLanguageCode()))
                            .findFirst()
                            .orElse(entity.getI18nEntries().isEmpty() ? null : entity.getI18nEntries().get(0)));
            if (i18n != null) {
                to.setCommonName(i18n.getCommonName());
                to.setReferenceUrl(i18n.getReferenceUrl());
            }
        }
        return to;
    }

    /**
     * Convert a coral stock entry to the public report's coral representation.
     */
    public PublicReefReportCoralTo toReportCoralTo(TankCoralStockEntity entity,
                                                    Map<String, BigDecimal> latestGrowthByType,
                                                    String latestPolypCondition) {
        if (entity == null) return null;
        PublicReefReportCoralTo to = new PublicReefReportCoralTo();
        to.setSpeciesName(entity.getSpeciesName());
        to.setClassification(entity.getClassification());
        to.setLatestGrowthByType(latestGrowthByType);
        to.setLatestPolypCondition(latestPolypCondition);
        return to;
    }
}

