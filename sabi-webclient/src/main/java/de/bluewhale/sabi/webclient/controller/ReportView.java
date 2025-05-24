/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
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

import static de.bluewhale.sabi.webclient.utils.PageRegister.REPORT_VIEW_PAGE;

/**
 * Controller for the Report View as shown in reportView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class ReportView extends AbstractControllerTools implements Serializable {

    // Limit drawn graph to avoid steady visual narrowing of target range by global minimum or maximum measurements
    final static int MAX_GRAPH_RECORDS = 14;
    @Autowired
    TankService tankService;

    @Autowired
    MeasurementService measurementService;

    @Inject
    UserSession userSession;

    // View Modell
    private List<AquariumTo> tanks;
    private List<UnitTo> knownUnits;
    private Long selectedTankId;
    private Integer selectedUnitId;
    private String lineModelRecentDays;
    private String lineModel90d;
    private String lineModel365d;

    private List<MeasurementTo> measurementTos;


    public List<MeasurementTo> getDescSortedMeasurementTos() {
        if (measurementTos != null) {
            return measurementTos.stream().sorted(Comparator.comparing(MeasurementTo::getMeasuredOn).reversed()).collect(Collectors.toList());
        } else {
            return new ArrayList<MeasurementTo>();
        }
    }

    public String requestData() {

        if (selectedTankId == null || selectedUnitId == null) {
            MessageUtil.info("messages", "common.no_such_data.t", userSession.getLocale());
            return REPORT_VIEW_PAGE.getNavigationableAddress();
        }

        try {
            measurementTos = measurementService.getMeasurementsForUsersTankFilteredByUnit(userSession.getSabiBackendToken(), selectedTankId, selectedUnitId);

            if (measurementTos == null || measurementTos.isEmpty()) {
                return REPORT_VIEW_PAGE.getNavigationableAddress();
            }

            // Ensure that measured points are sorted
            Comparator<MeasurementTo> measuredOnComperator = Comparator.comparing(MeasurementTo::getMeasuredOn);
            Collections.sort(measurementTos, measuredOnComperator);

            // Prepare the chart models
            List<BigDecimal> values_recent_measuredPoints = new ArrayList<BigDecimal>();
            List<String> labels_recent_measuredPoints = new ArrayList<>();

            List<BigDecimal> values_last90days = new ArrayList<BigDecimal>();
            List<String> labels_last90days = new ArrayList<>();

            List<BigDecimal> values_last365days = new ArrayList<BigDecimal>();
            List<String> labels_last365days = new ArrayList<>();

            // Slice available data to display in charts. As the source set has been presorted we don't need to sort again.
            List<MeasurementTo> measurementTos90d = measurementTos.stream().filter(item -> item.getMeasuredOn().isAfter(LocalDateTime.now().minusDays(90l))).collect(Collectors.toList());
            List<MeasurementTo> measurementTos365d = measurementTos.stream().filter(item -> item.getMeasuredOn().isAfter(LocalDateTime.now().minusDays(365l))).collect(Collectors.toList());

            String label = getUnitSignForId(selectedUnitId, knownUnits);

            // =======================================================================================
            // Graph Modell for last recent MAX_GRAPH_RECORDS measured points, with interpolated lines
            // =======================================================================================
            int size = measurementTos.size();
            for (MeasurementTo measurement : measurementTos) {
                // we want to display only the lastest MAX_GRAPH_RECORDS of the list
                if (size == 0) {
                    break;
                }
                if (size <= MAX_GRAPH_RECORDS) {
                    values_recent_measuredPoints.add(BigDecimal.valueOf(measurement.getMeasuredValue()));
                    labels_recent_measuredPoints.add(measurement.getMeasuredOn().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                size--;
            }

            lineModelRecentDays = generateJsonChartObject(values_recent_measuredPoints,labels_recent_measuredPoints,label,true);

            // =============================================================================
            // Graph Modell for last 90 days, lines not connected if we have null value gaps
            // =============================================================================

            // Lable start of each month
            // As of https://github.com/primefaces/primefaces/issues/9339
            // I chose to use the lable on several subsequent datapoints to make
            // sure that the smart chartJS render algorithm prints at least one of them in this case
            for (int dayOffset = 90; dayOffset > 0; dayOffset--) {

                LocalDate date = LocalDate.now().minusDays(dayOffset);
                int dayOfMonth = date.getDayOfMonth();

                // Lable start of each month
                if (dayOfMonth < 3) {
                    String monthi18nDisplayName = date.getMonth().getDisplayName(TextStyle.FULL, userSession.getLocale());
                    labels_last90days.add(monthi18nDisplayName);
                } else {
                    labels_last90days.add(Integer.toString(dayOfMonth));
                }

                // Add a value if one exists for the day (we take only the first value for given date)
                Optional<MeasurementTo> optMeasurementTo = measurementTos90d.stream().filter(item -> item.getMeasuredOn().toLocalDate().isEqual(date)).findFirst();
                if (optMeasurementTo.isPresent()) {
                    values_last90days.add(BigDecimal.valueOf(optMeasurementTo.get().getMeasuredValue()));
                } else {
                    values_last90days.add(null);
                }
            }

            lineModel90d = generateJsonChartObject(values_last90days,labels_last90days,label,true);

            // =============================================================================
            // Graph Modell for last 365 days, lines not connected if we have null value gaps
            // =============================================================================

            for (int dayOffset = 365; dayOffset > 0; dayOffset--) {

                LocalDate date = LocalDate.now().minusDays(dayOffset);
                int dayOfMonth = date.getDayOfMonth();

                // Lable start of each month
                // As of https://github.com/primefaces/primefaces/issues/9339
                // I chose to use the lable on several subsequent datapoints to make
                // sure that the smart chartJS render algorithm prints at least one of them in this case
                if (dayOfMonth < 8) {
                    String monthi18nDisplayName = date.getMonth().getDisplayName(TextStyle.FULL, userSession.getLocale());
                    String shortYear = " '"+Integer.toString(date.getYear() - 2000);
                    labels_last365days.add(monthi18nDisplayName+shortYear);
                } else {
                    labels_last365days.add(Strings.EMPTY);
                }

                // Add a value if one exists for the day (we take only the first value for given date)
                Optional<MeasurementTo> optMeasurementTo = measurementTos365d.stream().filter(item -> item.getMeasuredOn().toLocalDate().isEqual(date)).findFirst();
                if (optMeasurementTo.isPresent()) {
                    values_last365days.add(BigDecimal.valueOf(optMeasurementTo.get().getMeasuredValue()));
                } else {
                    values_last365days.add(null);
                }
            }

            lineModel365d = generateJsonChartObject(values_last365days, labels_last365days, label, false);

        } catch (BusinessException e) {
            MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
            log.error("Could not fetch measurements for tank {} and unit {}: {}", selectedTankId, selectedUnitId, e.getMessage());
        }

        return REPORT_VIEW_PAGE.getNavigationableAddress();
    }

    private String generateJsonChartObject(List<BigDecimal> valuesList, List<String> labelsList, String graphLabel, boolean spanGAPs) {
        LineChart chart = new LineChart();
        LineData lineData = new LineData();
        LineDataset lineDataset = new LineDataset();

        // Setting Styling and Options
        LineOptions options = buildLineOptions();

        lineDataset.setData(valuesList);
        lineDataset.setFill(new Fill<Boolean>(false));
        lineDataset.setLabel(graphLabel);
        lineDataset.setBorderColor(Color.DARK_TURQUOISE);
        lineDataset.setSpanGaps(spanGAPs);
        lineDataset.setLineTension(0.1f);

        lineData.addDataset(lineDataset);
        lineData.setLabels(labelsList);

        // Add Threshold Lines if available
        addThresholdLinesToLineDataModell(lineData, valuesList.size());

        // Chart konfigurieren
        chart.setData(lineData);
        chart.setOptions(options);

        return chart.toJson();
    }

    private LineOptions buildLineOptions() {
        LineOptions options = new LineOptions();

        // Legende
        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition(Legend.Position.TOP);

        Title title = new Title();
        title.setDisplay(true);
        String chartTitle = String.format(MessageUtil.getFromMessageProperties("reportview.chart.h", userSession.getLocale()),
                getTankNameForId(selectedTankId, tanks));
        title.setText(chartTitle);

        options.setPlugins(new Plugins()
                .setTitle(title)
                .setLegend(legend));


        // Linien-Konfiguration
        LineElements lineElements = new LineElements();// Linien-Konfiguration
        Line line = new Line();
        line.setTension(0.1f);
        line.setBorderWidth(2);
        lineElements.setLine(line);
        options.setElements(lineElements);


        // Responsive Design
        options.setResponsive(true);
        options.setMaintainAspectRatio(true);

        // Animations
        options.setAnimations(new Animations(AnimationType.X, new Animation().setDuration(1000)));

        return options;
    }

    private void addThresholdLinesToLineDataModell(@NotNull LineData lineData, @NotNull int dataPointsToThresholded) {
        ParameterTo parameterTo = null;
        try {
            parameterTo = measurementService.getParameterFor(selectedUnitId, userSession.getLanguage(),userSession.getSabiBackendToken());
        } catch (Exception e) {
            log.error("Could not Reach Backend to access detail parameter infos for unit {}", selectedUnitId);
            log.error(e.getMessage(), e);
            return;
        }

        if (parameterTo != null) {
            if (log.isDebugEnabled()) {
                log.debug("Found Threshold infos for {}. Add them to the chart for {}",
                        parameterTo, getUnitSignForId(selectedUnitId, knownUnits));
            }

            // Minimum Threshold Line
            LineDataset minThresholdDataSet = new LineDataset();
            minThresholdDataSet.setLabel("min.");
            minThresholdDataSet.setBorderColor(Color.GREEN);
            minThresholdDataSet.setLineTension(0f);  // Gerade Linie für Threshold
            minThresholdDataSet.setBorderDash(Arrays.asList(5, 5));  // Gestrichelte Linie
            minThresholdDataSet.setPointRadius(Collections.singletonList(0));  // Keine Punkte anzeigen
            minThresholdDataSet.setFill(new Fill<Boolean>(false));
            minThresholdDataSet.setBorderWidth(1);

            // Maximum Threshold Line
            LineDataset maxThresholdDataSet = new LineDataset();
            maxThresholdDataSet.setLabel("max.");
            maxThresholdDataSet.setBorderColor(Color.RED);
            maxThresholdDataSet.setLineTension(0f);  // Gerade Linie für Threshold
            maxThresholdDataSet.setBorderDash(Arrays.asList(5, 5));  // Gestrichelte Linie
            maxThresholdDataSet.setPointRadius(Collections.singletonList(0));  // Keine Punkte anzeigen
            maxThresholdDataSet.setFill(new Fill<Boolean>(false));
            maxThresholdDataSet.setBorderWidth(1);

            // Threshold-Werte für jeden Datenpunkt generieren
            List<BigDecimal> min_values = new ArrayList<BigDecimal>();
            List<BigDecimal> max_values = new ArrayList<BigDecimal>();

            for (int i = 0; i < dataPointsToThresholded; i++) {
                min_values.add(BigDecimal.valueOf(parameterTo.getMinThreshold()));
                max_values.add(BigDecimal.valueOf(parameterTo.getMaxThreshold()));
            }

            minThresholdDataSet.setData(min_values);
            maxThresholdDataSet.setData(max_values);

            // Threshold Lines zum Chart hinzufügen
            lineData.addDataset(minThresholdDataSet);
            lineData.addDataset(maxThresholdDataSet);
        }
    }

    @PostConstruct
    public void init() {
        log.debug("Called postconstruct init of ReportView.");
        try {
            // TODO STS (12.04.21): Refactor this. To make use of the request scope here, this should went into
            // users session, it does not make sense that we to this in measureview and in report view again.
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            knownUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken(), userSession.getLanguage());
            if (tanks.size() == 1) {
                // default selection if user has only one tank
                this.selectedTankId = (tanks.get(0).getId());
            }
        } catch (BusinessException e) {
            tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            MessageUtil.warn("messages", "common.token.expired.t", userSession.getLocale());
        }
    }
}