/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralGrowthHistoryTo;
import de.bluewhale.sabi.model.CoralGrowthType;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSF CDI-Bean controller for coral growth history (table + chart toggle, US3).
 * Uses PrimeFaces 15 p:chart with a JSON string value (no LineChartModel API in PF15).
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralGrowthHistoryView implements Serializable {

    private static final String[] CHART_COLORS = {
            "#065f46", "#0369a1", "#b45309", "#7c3aed", "#be185d"
    };

    @Autowired
    CoralStockService coralStockService;

    @Inject
    UserSession userSession;

    @Inject
    CoralEntryNavContext coralEntryNavContext;

    private List<CoralGrowthHistoryTo> growthHistory = new ArrayList<>();
    private boolean chartMode = false;
    /** Chart.js JSON config string for p:chart value attribute (PrimeFaces 15+) */
    private String chartJson = "{}";

    /** New measurement form fields */
    private LocalDate newMeasuredOn = LocalDate.now();
    private CoralGrowthType newMeasurementType = CoralGrowthType.SIZE_CM;
    private BigDecimal newMeasurementValue;

    /** Record being edited (null = not in edit mode) */
    private CoralGrowthHistoryTo editingRecord;

    @PostConstruct
    public void init() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null) return;
        try {
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
            buildChartJson();
        } catch (BusinessException e) {
            log.error("Failed to load growth history for coral {}", coralId, e);
        }
    }

    public CoralGrowthType[] getGrowthTypes() {
        return CoralGrowthType.values();
    }

    public void toggleChartView() {
        chartMode = !chartMode;
        if (chartMode) buildChartJson();
    }

    private void buildChartJson() {
        try {
            // Group by measurement type (preserving insertion order)
            Map<String, List<CoralGrowthHistoryTo>> byType = growthHistory.stream()
                    .collect(Collectors.groupingBy(r -> r.getMeasurementType().name(),
                            LinkedHashMap::new, Collectors.toList()));

            // Labels: all distinct dates sorted ascending
            List<String> labels = growthHistory.stream()
                    .map(r -> r.getMeasuredOn().toString())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            List<Map<String, Object>> datasets = new ArrayList<>();
            int colorIdx = 0;
            for (Map.Entry<String, List<CoralGrowthHistoryTo>> entry : byType.entrySet()) {
                String color = CHART_COLORS[colorIdx++ % CHART_COLORS.length];
                Map<String, BigDecimal> dateToVal = new LinkedHashMap<>();
                for (CoralGrowthHistoryTo r : entry.getValue()) {
                    dateToVal.put(r.getMeasuredOn().toString(), r.getMeasurementValue());
                }
                List<Object> values = new ArrayList<>();
                for (String lbl : labels) {
                    values.add(dateToVal.getOrDefault(lbl, null));
                }
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("label", entry.getKey());
                ds.put("data", values);
                ds.put("borderColor", color);
                ds.put("fill", false);
                ds.put("tension", 0.3);
                datasets.add(ds);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("labels", labels);
            data.put("datasets", datasets);

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("type", "line");
            config.put("data", data);

            chartJson = new ObjectMapper().writeValueAsString(config);
        } catch (Exception e) {
            log.error("Failed to build chart JSON for coral growth history", e);
            chartJson = "{}";
        }
    }

    public void onAddGrowthRecord() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null || newMeasurementValue == null) return;
        CoralGrowthHistoryTo record = new CoralGrowthHistoryTo();
        record.setCoralStockEntryId(coralId);
        record.setMeasuredOn(newMeasuredOn != null ? newMeasuredOn : LocalDate.now());
        record.setMeasurementType(newMeasurementType);
        record.setMeasurementValue(newMeasurementValue);
        try {
            coralStockService.addGrowthRecord(coralId, record, userSession.getSabiBackendToken());
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
            buildChartJson();
            newMeasurementValue = null;
            newMeasuredOn = LocalDate.now();
        } catch (BusinessException e) {
            log.error("Failed to add growth record for coral {}", coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeleteGrowthRecord(CoralGrowthHistoryTo record) {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null) return;
        try {
            coralStockService.deleteGrowthRecord(coralId, record.getId(), userSession.getSabiBackendToken());
            // Reload from backend — getGrowthHistory returns Arrays.asList() (fixed-size, no remove support)
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
            buildChartJson();
        } catch (BusinessException e) {
            log.error("Failed to delete growth record {} for coral {}", record.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onStartEditRecord(CoralGrowthHistoryTo record) {
        this.editingRecord = record;
    }

    public void onSaveEditRecord() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null || editingRecord == null) return;
        try {
            coralStockService.updateGrowthRecord(coralId, editingRecord, userSession.getSabiBackendToken());
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
            buildChartJson();
            editingRecord = null;
        } catch (BusinessException e) {
            log.error("Failed to update growth record {} for coral {}", editingRecord.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
