/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.CoralGrowthHistoryEntity;
import de.bluewhale.sabi.persistence.model.CoralPolypConditionEntity;
import de.bluewhale.sabi.persistence.model.TankCoralStockEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for TankCoralStockEntity &lt;-&gt; CoralStockEntryTo and related sub-objects.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Component
public class CoralStockMapper {

    // -------------------------  Entity → TO  -------------------------

    public CoralStockEntryTo toTo(TankCoralStockEntity entity) {
        if (entity == null) return null;
        CoralStockEntryTo to = new CoralStockEntryTo();
        to.setId(entity.getId());
        to.setAquariumId(entity.getAquariumId());
        to.setSpeciesName(entity.getSpeciesName());
        to.setScientificName(entity.getScientificName());
        to.setClassification(entity.getClassification() != null
                ? CoralClassification.valueOf(entity.getClassification()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setExternalRefUrl(entity.getExternalRefUrl());
        to.setNotes(entity.getNotes());
        to.setAddedOn(entity.getAddedOn());
        to.setDepartedOn(entity.getDepartedOn());
        to.setDepartureReason(entity.getDepartureReason() != null
                ? CoralDepartureReason.valueOf(entity.getDepartureReason()) : null);
        to.setDepartureNote(entity.getDepartureNote());
        to.setCoralCatalogueId(entity.getCoralCatalogueId());
        return to;
    }

    public TankCoralStockEntity toEntity(CoralStockEntryTo to) {
        if (to == null) return null;
        TankCoralStockEntity entity = new TankCoralStockEntity();
        entity.setId(to.getId());
        entity.setAquariumId(to.getAquariumId());
        entity.setSpeciesName(to.getSpeciesName());
        entity.setScientificName(to.getScientificName());
        entity.setClassification(to.getClassification() != null ? to.getClassification().name() : null);
        entity.setCareLevel(to.getCareLevel() != null ? to.getCareLevel().name() : null);
        entity.setExternalRefUrl(to.getExternalRefUrl());
        entity.setNotes(to.getNotes());
        entity.setAddedOn(to.getAddedOn());
        entity.setDepartedOn(to.getDepartedOn());
        entity.setDepartureReason(to.getDepartureReason() != null ? to.getDepartureReason().name() : null);
        entity.setDepartureNote(to.getDepartureNote());
        entity.setCoralCatalogueId(to.getCoralCatalogueId());
        return entity;
    }

    public CoralGrowthHistoryTo toGrowthTo(CoralGrowthHistoryEntity entity) {
        if (entity == null) return null;
        CoralGrowthHistoryTo to = new CoralGrowthHistoryTo();
        to.setId(entity.getId());
        to.setCoralStockEntryId(entity.getCoralStockId());
        to.setMeasuredOn(entity.getMeasuredOn());
        to.setMeasurementType(CoralGrowthType.valueOf(entity.getMeasurementType()));
        to.setMeasurementValue(entity.getMeasurementValue());
        return to;
    }

    public CoralGrowthHistoryEntity toGrowthEntity(CoralGrowthHistoryTo to) {
        if (to == null) return null;
        CoralGrowthHistoryEntity entity = new CoralGrowthHistoryEntity();
        entity.setId(to.getId());
        entity.setCoralStockId(to.getCoralStockEntryId());
        entity.setMeasuredOn(to.getMeasuredOn());
        entity.setMeasurementType(to.getMeasurementType() != null ? to.getMeasurementType().name() : null);
        entity.setMeasurementValue(to.getMeasurementValue());
        return entity;
    }

    public CoralPolypConditionTo toPolypTo(CoralPolypConditionEntity entity) {
        if (entity == null) return null;
        CoralPolypConditionTo to = new CoralPolypConditionTo();
        to.setId(entity.getId());
        to.setCoralStockEntryId(entity.getCoralStockId());
        to.setObservedOn(entity.getObservedOn());
        to.setCondition(PolypCondition.valueOf(entity.getPolypCondition()));
        return to;
    }

    public CoralPolypConditionEntity toPolypEntity(CoralPolypConditionTo to) {
        if (to == null) return null;
        CoralPolypConditionEntity entity = new CoralPolypConditionEntity();
        entity.setId(to.getId());
        entity.setCoralStockId(to.getCoralStockEntryId());
        entity.setObservedOn(to.getObservedOn());
        entity.setPolypCondition(to.getCondition() != null ? to.getCondition().name() : null);
        return entity;
    }

    public CoralExportTo toExportTo(TankCoralStockEntity entity,
                                    List<CoralGrowthHistoryEntity> growthHistory,
                                    List<CoralPolypConditionEntity> polypHistory) {
        if (entity == null) return null;
        CoralExportTo exportTo = new CoralExportTo();
        exportTo.setCoralCatalogueId(entity.getCoralCatalogueId());
        exportTo.setScientificName(entity.getScientificName());
        exportTo.setSpeciesName(entity.getSpeciesName());
        exportTo.setClassification(entity.getClassification());
        exportTo.setAddedOn(entity.getAddedOn() != null ? entity.getAddedOn().toString() : null);
        exportTo.setDepartedOn(entity.getDepartedOn() != null ? entity.getDepartedOn().toString() : null);
        exportTo.setDepartureReason(entity.getDepartureReason());
        exportTo.setDepartureNote(entity.getDepartureNote());
        exportTo.setNotes(entity.getNotes());

        if (growthHistory != null) {
            growthHistory.forEach(g -> {
                CoralGrowthHistoryExportTo gExport = new CoralGrowthHistoryExportTo();
                gExport.setMeasuredOn(g.getMeasuredOn() != null ? g.getMeasuredOn().toString() : null);
                gExport.setMeasurementType(g.getMeasurementType());
                gExport.setMeasurementValue(g.getMeasurementValue());
                exportTo.getGrowthHistory().add(gExport);
            });
        }

        if (polypHistory != null) {
            polypHistory.forEach(p -> {
                CoralPolypConditionExportTo pExport = new CoralPolypConditionExportTo();
                pExport.setObservedOn(p.getObservedOn() != null ? p.getObservedOn().toString() : null);
                pExport.setCondition(p.getPolypCondition());
                exportTo.getPolypConditionHistory().add(pExport);
            });
        }

        return exportTo;
    }
}

