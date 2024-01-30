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
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
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
@SessionScope
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
    private LineChartModel lineModel_recent_measuredPoints;
    private LineChartModel lineModel90d;
    private LineChartModel lineModel365d;
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

            // reinitialize lineModel, old Data free for GC
            lineModel_recent_measuredPoints = new LineChartModel();
            lineModel90d = new LineChartModel();
            lineModel365d = new LineChartModel();

            //Options
            LineChartOptions options = new LineChartOptions();
            Title title = new Title();
            title.setDisplay(true);
            String chartTitle = String.format(MessageUtil.getFromMessageProperties("reportview.chart.h", userSession.getLocale()),
                    getTankNameForId(selectedTankId, tanks));
            title.setText(chartTitle);
            options.setTitle(title);

            lineModel_recent_measuredPoints.setOptions(options);
            lineModel90d.setOptions(options);
            lineModel365d.setOptions(options);

            LineChartDataSet dataSet_recent_measuredPoints = new LineChartDataSet();
            List<Object> values_recent_measuredPoints = new ArrayList<>();
            List<String> labels_recent_measuredPoints = new ArrayList<>();

            LineChartDataSet dataSet_last90days = new LineChartDataSet();
            List<Object> values_last90days = new ArrayList<>();
            List<String> labels_last90days = new ArrayList<>();

            LineChartDataSet dataSet_last365days = new LineChartDataSet();
            List<Object> values_last365days = new ArrayList<>();
            List<String> labels_last365days = new ArrayList<>();

            // Slice available data to to display in charts. As the source set has been presorted we don't need to sort again.
            List<MeasurementTo> measurementTos90d = measurementTos.stream().filter(item -> item.getMeasuredOn().isAfter(LocalDateTime.now().minusDays(90l))).collect(Collectors.toList());
            List<MeasurementTo> measurementTos365d = measurementTos.stream().filter(item -> item.getMeasuredOn().isAfter(LocalDateTime.now().minusDays(365l))).collect(Collectors.toList());

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
                    values_recent_measuredPoints.add(measurement.getMeasuredValue());
                    labels_recent_measuredPoints.add(measurement.getMeasuredOn().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                size--;
            }

            populteDataSet(dataSet_recent_measuredPoints,values_recent_measuredPoints,labels_recent_measuredPoints,lineModel_recent_measuredPoints,true);

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
                    values_last90days.add(optMeasurementTo.get().getMeasuredValue());
                } else {
                    values_last90days.add(null);
                }
            }

            populteDataSet(dataSet_last90days,values_last90days,labels_last90days,lineModel90d, false);

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
                    values_last365days.add(optMeasurementTo.get().getMeasuredValue());
                } else {
                    values_last365days.add(null);
                }
            }

            populteDataSet(dataSet_last365days, values_last365days, labels_last365days, lineModel365d, false);

        } catch (BusinessException e) {
            MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
            e.printStackTrace();
        }

        return REPORT_VIEW_PAGE.getNavigationableAddress();
    }

    private void populteDataSet(LineChartDataSet lineChartDataSet, List<Object> valuesList, List<String> labelsList, LineChartModel lineChartModel, boolean spanGAPs) {
        lineChartDataSet.setData(valuesList);
        lineChartDataSet.setFill(false);
        lineChartDataSet.setLabel(getUnitSignForId(selectedUnitId, knownUnits));
        lineChartDataSet.setBorderColor("rgb(75, 192, 192)");
        lineChartDataSet.setTension(0.1);

        // false = do not connect points if we got a null value. So if not daily measured this will result in a pointed graph
        lineChartDataSet.setSpanGaps(spanGAPs);

        ChartData chartData = new ChartData();
        chartData.setLabels(labelsList);
        chartData.addChartDataSet(lineChartDataSet);

        // Add Threshold Lines if threshold data is available
        applyThresholdLineToLineChartModell(chartData, lineChartModel);
    }

    private void applyThresholdLineToLineChartModell(@NotNull ChartData chartData, @NotNull LineChartModel lineChartModel) {

        ParameterTo parameterTo = null;
        try {
            parameterTo = measurementService.getParameterFor(selectedUnitId, userSession.getSabiBackendToken());
        } catch (Exception e) {
            log.error("Could not Reach Backend to access detail parameter infos for unit {}", selectedUnitId);
            e.printStackTrace();
        }

        if (parameterTo != null) {

            if (log.isDebugEnabled()) {
                log.debug("Found Threshold infos for {}. Add them to the chart for {}",
                        parameterTo, getUnitSignForId(selectedUnitId, knownUnits));
            }

            LineChartDataSet minThresholdDataSet = new LineChartDataSet();
            minThresholdDataSet.setLabel("min.");
            minThresholdDataSet.setBorderColor("rgb(0, 192, 0)");
            minThresholdDataSet.setTension(0.2);
            minThresholdDataSet.setShowLine(true);

            LineChartDataSet maxThresholdDataSet = new LineChartDataSet();
            maxThresholdDataSet.setLabel("max.");
            maxThresholdDataSet.setBorderColor("rgb(192, 0, 0)");
            maxThresholdDataSet.setTension(0.2);
            maxThresholdDataSet.setShowLine(true);

            List<Object> min_values = new ArrayList<>();
            List<Object> max_values = new ArrayList<>();

            LineChartDataSet chartDataSet = (LineChartDataSet) chartData.getDataSet().get(0);
            int size = chartDataSet.getData().size();

            if (log.isDebugEnabled()) {
                log.debug("Generate {} dataPoints to draw threshold values", size);
            }

            for (int i = 0; i < size; i++) {
                min_values.add(parameterTo.getMinThreshold());
                max_values.add(parameterTo.getMaxThreshold());
            }

            minThresholdDataSet.setData(min_values);
            maxThresholdDataSet.setData(max_values);
            chartData.addChartDataSet(minThresholdDataSet);
            chartData.addChartDataSet(maxThresholdDataSet);
        }

        lineChartModel.setData(chartData);
    }

    @PostConstruct
    public void init() {
        log.debug("Called postconstruct init of ReportView.");
        try {
            // TODO STS (12.04.21): Refactor this. To make use of the request scope here, this should went into
            // users session, it does not make sense that we to this in measureview and in report view again.
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            knownUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken());
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
