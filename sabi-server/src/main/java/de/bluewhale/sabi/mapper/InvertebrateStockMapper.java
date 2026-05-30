/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.mapper;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.InvertebrateWaterSensitivityEntity;
import de.bluewhale.sabi.persistence.model.TankInvertebrateStockEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for TankInvertebrateStockEntity &lt;-&gt; InvertebrateStockEntryTo and export TOs.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Component
public class InvertebrateStockMapper {

    // -------------------------  Entity → TO  -------------------------

    public InvertebrateStockEntryTo toTo(TankInvertebrateStockEntity entity) {
        if (entity == null) return null;
        InvertebrateStockEntryTo to = new InvertebrateStockEntryTo();
        to.setId(entity.getId());
        to.setAquariumId(entity.getAquariumId());
        to.setSpeciesName(entity.getSpeciesName());
        to.setScientificName(entity.getScientificName());
        to.setTaxonomicCategory(entity.getTaxonomicCategory() != null
                ? InvertebrateTaxonomicCategory.valueOf(entity.getTaxonomicCategory()) : null);
        to.setCareLevel(entity.getCareLevel() != null
                ? CoralCareLevel.valueOf(entity.getCareLevel()) : null);
        to.setMobility(entity.getMobility() != null
                ? InvertebrateMobility.valueOf(entity.getMobility()) : null);
        to.setEcologicalRole(entity.getEcologicalRole() != null
                ? InvertebrateEcologicalRole.valueOf(entity.getEcologicalRole()) : null);
        to.setActivityPattern(entity.getActivityPattern() != null
                ? InvertebrateActivityPattern.valueOf(entity.getActivityPattern()) : null);
        to.setExternalRefUrl(entity.getExternalRefUrl());
        to.setNotes(entity.getNotes());
        to.setAddedOn(entity.getAddedOn());
        to.setDepartedOn(entity.getDepartedOn());
        to.setDepartureReason(entity.getDepartureReason() != null
                ? CoralDepartureReason.valueOf(entity.getDepartureReason()) : null);
        to.setDepartureNote(entity.getDepartureNote());
        to.setInvertebrateCatalogueId(entity.getInvertebrateCatalogueId());
        // Map water sensitivities
        if (entity.getWaterSensitivities() != null) {
            List<Integer> unitIds = entity.getWaterSensitivities().stream()
                    .map(InvertebrateWaterSensitivityEntity::getUnitId)
                    .collect(Collectors.toList());
            to.setWaterSensitivityUnitIds(unitIds);
        }
        return to;
    }

    public TankInvertebrateStockEntity toEntity(InvertebrateStockEntryTo to) {
        if (to == null) return null;
        TankInvertebrateStockEntity entity = new TankInvertebrateStockEntity();
        entity.setId(to.getId());
        entity.setAquariumId(to.getAquariumId());
        entity.setSpeciesName(to.getSpeciesName());
        entity.setScientificName(to.getScientificName());
        entity.setTaxonomicCategory(to.getTaxonomicCategory() != null ? to.getTaxonomicCategory().name() : null);
        entity.setCareLevel(to.getCareLevel() != null ? to.getCareLevel().name() : null);
        entity.setMobility(to.getMobility() != null ? to.getMobility().name() : null);
        entity.setEcologicalRole(to.getEcologicalRole() != null ? to.getEcologicalRole().name() : null);
        entity.setActivityPattern(to.getActivityPattern() != null ? to.getActivityPattern().name() : null);
        entity.setExternalRefUrl(to.getExternalRefUrl());
        entity.setNotes(to.getNotes());
        entity.setAddedOn(to.getAddedOn());
        entity.setDepartedOn(to.getDepartedOn());
        entity.setDepartureReason(to.getDepartureReason() != null ? to.getDepartureReason().name() : null);
        entity.setDepartureNote(to.getDepartureNote());
        entity.setInvertebrateCatalogueId(to.getInvertebrateCatalogueId());
        return entity;
    }

    /**
     * Create water sensitivity entities from unit IDs (actual persistence handled by service).
     */
    public List<InvertebrateWaterSensitivityEntity> toWaterSensitivityEntities(Long invertebrateStockId, List<Integer> unitIds) {
        if (unitIds == null) return new ArrayList<>();
        return unitIds.stream().map(unitId -> {
            InvertebrateWaterSensitivityEntity e = new InvertebrateWaterSensitivityEntity();
            e.setInvertebrateStockId(invertebrateStockId);
            e.setUnitId(unitId);
            return e;
        }).collect(Collectors.toList());
    }

    /**
     * Map entity to export TO for AI-JSON export.
     */
    public InvertebrateExportTo toExportTo(TankInvertebrateStockEntity entity) {
        if (entity == null) return null;
        InvertebrateExportTo exportTo = new InvertebrateExportTo();
        exportTo.setCatalogueId(entity.getInvertebrateCatalogueId());
        exportTo.setScientificName(entity.getScientificName());
        exportTo.setSpeciesName(entity.getSpeciesName());
        exportTo.setTaxonomicCategory(entity.getTaxonomicCategory());
        exportTo.setAddedOn(entity.getAddedOn() != null ? entity.getAddedOn().toString() : null);
        exportTo.setDepartedOn(entity.getDepartedOn() != null ? entity.getDepartedOn().toString() : null);
        exportTo.setDepartureReason(entity.getDepartureReason());
        exportTo.setDepartureNote(entity.getDepartureNote());
        exportTo.setNotes(entity.getNotes());
        exportTo.setMobility(entity.getMobility());
        exportTo.setEcologicalRole(entity.getEcologicalRole());
        exportTo.setActivityPattern(entity.getActivityPattern());
        // Water sensitivity units mapped by caller (service injects unit names)
        return exportTo;
    }
}
