/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.CDIBeans.ApplicationInfo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import lombok.Getter;
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
public class ReportView implements Serializable {

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

    public String requestData() {

        if (selectedTankId == null || selectedUnitId == null) {
            MessageUtil.info("messages", "common.no_such_data.t", userSession.getLocale());
            return REPORT_VIEW_PAGE;
        }

        try {
            List<MeasurementTo> measurementTos = measurementService.getMeasurementsForUsersTankFilteredByUnit(userSession.getSabiBackendToken(), selectedTankId, selectedUnitId);
            // TODO STS (13.04.21): Transform data into chart line modell, also data table for export

            lineModel = new LineChartModel();
            ChartData data = new ChartData();

            LineChartDataSet dataSet = new LineChartDataSet();
            List<Object> values = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            for (MeasurementTo measurement : measurementTos) {
                values.add(measurement.getMeasuredValue());
                labels.add(measurement.getMeasuredOn().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            dataSet.setData(values);
            data.setLabels(labels);
            dataSet.setFill(false);
            dataSet.setLabel(ApplicationInfo.getUnitSignForId(selectedUnitId, knownUnits));
            dataSet.setBorderColor("rgb(75, 192, 192)");
            dataSet.setLineTension(0.1);
            data.addChartDataSet(dataSet);

            //Options
            LineChartOptions options = new LineChartOptions();
            Title title = new Title();
            title.setDisplay(true);
            // TODO STS (13.04.21): i18n
            String chartTitle = String.format(MessageUtil.getFromMessageProperties("reportview.chart.h", userSession.getLocale()),
                    ApplicationInfo.getTankNameForId(selectedTankId, tanks));
            title.setText(chartTitle);
            options.setTitle(title);

            lineModel.setOptions(options);
            lineModel.setData(data);


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
