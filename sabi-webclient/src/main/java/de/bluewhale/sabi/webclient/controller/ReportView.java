/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    private static final String REPORT_VIEW_PAGE = "reportView";
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
    private LineChartModel lineModel;
    private List<MeasurementTo> measurementTos;

    public String requestData() {

        if (selectedTankId == null || selectedUnitId == null) {
            MessageUtil.info("messages", "common.no_such_data.t", userSession.getLocale());
            return REPORT_VIEW_PAGE;
        }

        try {
            measurementTos = measurementService.getMeasurementsForUsersTankFilteredByUnit(userSession.getSabiBackendToken(), selectedTankId, selectedUnitId);

            if (measurementTos == null || measurementTos.isEmpty()) {
                return REPORT_VIEW_PAGE;
            }

            Comparator<MeasurementTo> measuredOnComperator = Comparator.comparing(MeasurementTo::getMeasuredOn);
            Collections.sort(measurementTos, measuredOnComperator);

            // reinitialize lineModel, old Data free for GC
            lineModel = new LineChartModel();

            //Options
            LineChartOptions options = new LineChartOptions();
            Title title = new Title();
            title.setDisplay(true);
            String chartTitle = String.format(MessageUtil.getFromMessageProperties("reportview.chart.h", userSession.getLocale()),
                    getTankNameForId(selectedTankId, tanks));
            title.setText(chartTitle);
            options.setTitle(title);
            lineModel.setOptions(options);

            LineChartDataSet dataSet = new LineChartDataSet();
            List<Object> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int size = measurementTos.size();
            for (MeasurementTo measurement : measurementTos) {
                // we want to display only the lastest MAX_GRAPH_RECORDS of the list
                if (size == 0) {
                    break;
                }
                if (size <= MAX_GRAPH_RECORDS) {
                    values.add(measurement.getMeasuredValue());
                    labels.add(measurement.getMeasuredOn().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
                size--;
            }

            dataSet.setData(values);
            dataSet.setFill(false);
            dataSet.setLabel(getUnitSignForId(selectedUnitId, knownUnits));
            dataSet.setBorderColor("rgb(75, 192, 192)");
            dataSet.setLineTension(0.1);

            ChartData chartData = new ChartData();
            chartData.setLabels(labels);
            chartData.addChartDataSet(dataSet);

            // Add Threshold Lines if threshold data is available
            ParameterTo parameterTo = null;
            try {
                parameterTo = measurementService.getParameterFor(selectedUnitId, userSession.getSabiBackendToken());
            } catch (Exception e) {
                log.error("Could not Reach Backend to access detail parameter infos for unit {}", selectedUnitId);
                e.printStackTrace();
            }

            if (parameterTo != null) {

                log.debug("Found Threshold infos for {}. Add them to the chart for {}",
                        parameterTo, getUnitSignForId(selectedUnitId, knownUnits));

                LineChartDataSet minThresholdDataSet = new LineChartDataSet();
                minThresholdDataSet.setLabel("min.");
                minThresholdDataSet.setBorderColor("rgb(0, 192, 0)");
                minThresholdDataSet.setLineTension(0.2);
                minThresholdDataSet.setShowLine(true);

                LineChartDataSet maxThresholdDataSet = new LineChartDataSet();
                maxThresholdDataSet.setLabel("max.");
                maxThresholdDataSet.setBorderColor("rgb(192, 0, 0)");
                maxThresholdDataSet.setLineTension(0.2);
                maxThresholdDataSet.setShowLine(true);

                List<Object> min_values = new ArrayList<>();
                List<Object> max_values = new ArrayList<>();

                for (int i = 0; i < MAX_GRAPH_RECORDS; i++) {
                    min_values.add(parameterTo.getMinThreshold());
                    max_values.add(parameterTo.getMaxThreshold());
                }

                minThresholdDataSet.setData(min_values);
                maxThresholdDataSet.setData(max_values);
                chartData.addChartDataSet(minThresholdDataSet);
                chartData.addChartDataSet(maxThresholdDataSet);
            }

            lineModel.setData(chartData);

        } catch (BusinessException e) {
            MessageUtil.error("messages", "common.error.internal_server_problem.t", userSession.getLocale());
            e.printStackTrace();
        }

        return REPORT_VIEW_PAGE;
    }

    @PostConstruct
    public void init() {
        log.debug("Called postconstruct init of ReportView.");
        try {
            // TODO STS (12.04.21): Refactor this. To make use of the request scope here, this should went into
            // users session, it does not make sense that we to this in measureview and in report view again.
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            knownUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            MessageUtil.warn("messages", "common.token.expired.t", userSession.getLocale());
        }
    }


}
