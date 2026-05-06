/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.PublicReportService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.Color;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.animation.Animation;
import software.xdev.chartjs.model.options.animation.AnimationType;
import software.xdev.chartjs.model.options.animation.Animations;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.elements.Line;
import software.xdev.chartjs.model.options.elements.LineElements;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSF backing bean for the public HouseReef report page.
 * Loaded via a {@code f:viewParam} from the URL query parameter {@code token}.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class HouseReefReportView implements Serializable {

    @Inject
    UserSession userSession;

    @Autowired
    PublicReportService publicReportService;

    /** Share token read from the URL query parameter "token". */
    private String token;

    /** The assembled report data (populated in {@link #loadReport()}). */
    private PublicReefReportTo report;

    /**
     * Measurement charts: one JSON chart model per unit (unitId → chartJson).
     * Populated in {@link #loadReport()}.
     */
    private Map<Integer, String> chartModels = new LinkedHashMap<>();

    /** Ordered list of unit IDs for iteration in the view. */
    private List<Integer> unitIds = new ArrayList<>();

    /**
     * Called by {@code f:event type="preRenderView"} after view params are applied.
     * Fetches the report from the backend.
     */
    public void loadReport() {
        if (token == null || token.isBlank()) {
            report = buildExpiredReport();
            return;
        }
        try {
            report = publicReportService.getReport(token, userSession.getLanguage());
            buildChartModels();
        } catch (BusinessException e) {
            log.error("Failed to load public report for token {}: {}", token, e.getMessage());
            report = buildExpiredReport();
        }
    }

    /** Convenience accessor for the XHTML to check whether the report has data. */
    public boolean isReportAvailable() {
        return report != null && !report.isLinkExpired();
    }

    /** Returns the unit sign for the given unitId. */
    public String getUnitSign(Integer unitId) {
        if (report == null || report.getUnitMap() == null) return "";
        UnitTo unit = report.getUnitMap().get(unitId);
        return unit != null ? unit.getUnitSign() : String.valueOf(unitId);
    }

    /** Returns the chart JSON for the given unitId, or null if no measurements. */
    public String getChartFor(Integer unitId) {
        return chartModels.get(unitId);
    }

    // ---- Private helpers ----

    private PublicReefReportTo buildExpiredReport() {
        PublicReefReportTo r = new PublicReefReportTo();
        r.setLinkExpired(true);
        r.setReportGeneratedAt(LocalDateTime.now());
        return r;
    }

    private void buildChartModels() {
        if (report == null || report.getMeasurementsByUnit() == null) return;

        unitIds = new ArrayList<>(report.getMeasurementsByUnit().keySet());

        for (Map.Entry<Integer, List<MeasurementTo>> entry : report.getMeasurementsByUnit().entrySet()) {
            Integer unitId = entry.getKey();
            List<MeasurementTo> measurements = entry.getValue();

            if (measurements == null || measurements.isEmpty()) continue;

            // Build 90-day chart (last 3 months)
            List<BigDecimal> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            Locale locale = userSession.getLocale();

            for (int dayOffset = 90; dayOffset >= 0; dayOffset--) {
                LocalDate date = LocalDate.now().minusDays(dayOffset);
                int dayOfMonth = date.getDayOfMonth();
                if (dayOfMonth < 3) {
                    labels.add(date.getMonth().getDisplayName(TextStyle.FULL, locale));
                } else {
                    labels.add(Integer.toString(dayOfMonth));
                }

                Optional<MeasurementTo> opt = measurements.stream()
                        .filter(m -> m.getMeasuredOn() != null
                                && m.getMeasuredOn().toLocalDate().isEqual(date))
                        .findFirst();
                values.add(opt.map(m -> BigDecimal.valueOf(m.getMeasuredValue())).orElse(null));
            }

            String unitSign = getUnitSign(unitId);
            String chartJson = buildLineChartJson(values, labels, unitSign);
            chartModels.put(unitId, chartJson);
        }
    }

    private String buildLineChartJson(List<BigDecimal> values, List<String> labels, String unitSign) {
        LineChart chart = new LineChart();
        LineData lineData = new LineData();
        LineDataset dataset = new LineDataset();

        dataset.setData(values);
        dataset.setFill(new Fill<>(false));
        dataset.setLabel(unitSign);
        dataset.setBorderColor(Color.DARK_TURQUOISE);
        dataset.setSpanGaps(false);
        dataset.setLineTension(0.1f);

        lineData.addDataset(dataset);
        lineData.setLabels(labels);

        LineOptions options = new LineOptions();
        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition(Legend.Position.TOP);
        Title title = new Title();
        title.setDisplay(true);
        title.setText(unitSign);
        options.setPlugins(new Plugins().setTitle(title).setLegend(legend));

        LineElements lineElements = new LineElements();
        Line line = new Line();
        line.setTension(0.1f);
        line.setBorderWidth(2);
        lineElements.setLine(line);
        options.setElements(lineElements);
        options.setResponsive(true);
        options.setMaintainAspectRatio(true);
        options.setAnimations(new Animations(AnimationType.X, new Animation().setDuration(1000)));

        chart.setData(lineData);
        chart.setOptions(options);
        return chart.toJson();
    }
}
