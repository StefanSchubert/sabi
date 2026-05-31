/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueEntity;
import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueI18nEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for InvertebrateCatalogueEntity &lt;-&gt; InvertebrateCatalogueEntryTo and search results.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Component
public class InvertebrateCatalogueMapper {

    public InvertebrateCatalogueEntryTo toTo(InvertebrateCatalogueEntity entity) {
        if (entity == null) return null;
        InvertebrateCatalogueEntryTo to = new InvertebrateCatalogueEntryTo();
        to.setId(entity.getId());
        to.setScientificName(entity.getScientificName());
        to.setTaxonomicCategory(entity.getTaxonomicCategory() != null
                ? InvertebrateTaxonomicCategory.valueOf(entity.getTaxonomicCategory()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setStatus(entity.getStatus() != null ? FishCatalogueStatus.valueOf(entity.getStatus()) : null);
        to.setProposerUserId(entity.getProposerUserId());
        to.setProposalDate(entity.getProposalDate());

        if (entity.getI18nEntries() != null) {
            List<InvertebrateCatalogueI18nTo> i18nTos = new ArrayList<>();
            entity.getI18nEntries().forEach(i18n -> i18nTos.add(toI18nTo(i18n)));
            to.setI18nEntries(i18nTos);
        }
        return to;
    }

    public InvertebrateCatalogueI18nTo toI18nTo(InvertebrateCatalogueI18nEntity entity) {
        if (entity == null) return null;
        InvertebrateCatalogueI18nTo to = new InvertebrateCatalogueI18nTo();
        to.setId(entity.getId());
        to.setLanguageCode(entity.getLanguageCode());
        to.setCommonName(entity.getCommonName());
        to.setDescription(entity.getDescription());
        to.setReferenceUrl(entity.getReferenceUrl());
        return to;
    }

    public InvertebrateCatalogueEntity toEntity(InvertebrateCatalogueEntryTo to) {
        if (to == null) return null;
        InvertebrateCatalogueEntity entity = new InvertebrateCatalogueEntity();
        entity.setId(to.getId());
        entity.setScientificName(to.getScientificName());
        entity.setTaxonomicCategory(to.getTaxonomicCategory() != null ? to.getTaxonomicCategory().name() : null);
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
    public InvertebrateCatalogueSearchResultTo toSearchResult(InvertebrateCatalogueEntity entity, String lang) {
        if (entity == null) return null;
        InvertebrateCatalogueSearchResultTo to = new InvertebrateCatalogueSearchResultTo();
        to.setId(entity.getId());
        to.setScientificName(entity.getScientificName());
        to.setTaxonomicCategory(entity.getTaxonomicCategory() != null
                ? InvertebrateTaxonomicCategory.valueOf(entity.getTaxonomicCategory()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setStatus(entity.getStatus() != null ? FishCatalogueStatus.valueOf(entity.getStatus()) : null);

        if (entity.getI18nEntries() != null) {
            InvertebrateCatalogueI18nEntity i18n = entity.getI18nEntries().stream()
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
}
