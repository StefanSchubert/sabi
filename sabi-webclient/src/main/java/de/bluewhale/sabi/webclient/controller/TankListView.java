/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Tanklist View as shown in userportal.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
@Getter
public class TankListView implements Serializable {

    @Autowired
    TankService tankService;
    @Inject
    UserSession userSession;
    private List<AquariumTo> tanks;
    private AquariumTo selectedTank;

    @PostConstruct
    public void init() {
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            FacesContext.getCurrentInstance().addMessage("Exception", new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", "Backendtoken expired? Please relogin."));
        }
    }

    public void select(AquariumTo tank) {
        selectedTank = tank;
    }

    public void addTank() {
        selectedTank = new AquariumTo();
    }

    public void delete(AquariumTo tank) {
        try {
            tankService.deleteTankById(tank.getId(), userSession.getSabiBackendToken());
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            // Leave list untouched in this case.
            // tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            FacesContext.getCurrentInstance().addMessage("Exception", new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", e.getLocalizedMessage()));
        }
    }

    public void save() {
        // TODO STS (13.09.20):
        // merge selected with backend and reload list
        // notice could also be a new one, check handling of IDs in this case.
    }

}
