/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Controller for the Measurement View as shown in measureView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
@Getter
public class MeasurementListView implements Serializable {

    private static final String MEASUREMENT_VIEW_PAGE = "measureView";

    @Autowired
    MeasurementService measurementService;

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    // View Modell
    private List<AquariumTo> tanks;
    private List<UnitTo> knownUnits;
    private MeasurementTo measurement = new MeasurementTo();

    @PostConstruct
    public void init() {
        // user should be able to choose from his tanks
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            if (tanks.size() == 1) {
                // default selection if user has only one tank
                measurement.setAquariumId(tanks.get(0).getId());
            }
        } catch (BusinessException e) {
            tanks = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            FacesContext.getCurrentInstance().addMessage("Exception", new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", "Backendtoken expired? Please relogin."));
        }

        try {
            knownUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            knownUnits = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            FacesContext.getCurrentInstance().addMessage("Exception", new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", "Backendtoken expired? Please relogin."));
        }


    }

    //
//    public String edit(AquariumTo tank) {
//        selectedTank = tank;
//        return TANK_EDITOR_PAGE;
//    }
//
//    public String addTank() {
//        selectedTank = new AquariumTo();
//        return TANK_EDITOR_PAGE;
//    }
//
    public String resetForm() {
        measurement = new MeasurementTo();
        return MEASUREMENT_VIEW_PAGE;
    }
//
//    public String save() {
//        if (selectedTank != null) {
//            // Already stored
//            try {
//                tankService.save(selectedTank, userSession.getSabiBackendToken());
//                tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
//            } catch (BusinessException e) {
//                e.printStackTrace();
//                FacesContext.getCurrentInstance().addMessage("Exception", new FacesMessage(FacesMessage.SEVERITY_WARN, "Sorry!",
//                        MessageUtil.getFromMessageProperties("common.error.internal_server_problem",userSession.getLocale())));
//            }
//        }
//        return MEASUREMENT_VIEW_PAGE;
//    }

}
