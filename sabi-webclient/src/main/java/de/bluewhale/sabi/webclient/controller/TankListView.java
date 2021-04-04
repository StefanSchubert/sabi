/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Tanklist View as shown in tankView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
@Getter
public class TankListView implements Serializable {

    private static final String TANK_EDITOR_PAGE = "tankEditor";
    private static final String TANK_VIEW_PAGE = "tankView";

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    private List<AquariumTo> tanks;
    private AquariumTo selectedTank;

    @PostConstruct
    public void init() {
        log.debug("Called postconstruct init of TankListView.");
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            MessageUtil.warn("messages","common.token.expired.t",userSession.getLocale());
        }
    }

    @PreDestroy
    public void endOfService() {
        log.debug("Called predestroy on TankListView");
    }

    public String edit(AquariumTo tank) {
        selectedTank = tank;
        return TANK_EDITOR_PAGE;
    }

    public String addTank() {
        selectedTank = new AquariumTo();
        return TANK_EDITOR_PAGE;
    }

    public void delete(AquariumTo tank) {
        try {
            tankService.deleteTankById(tank.getId(), userSession.getSabiBackendToken());
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            // Leave list untouched in this case.
            // tanks = new ArrayList<>();
            log.error(e.getLocalizedMessage());
            MessageUtil.warn("messages","common.error.internal_server_problem.t",userSession.getLocale());
        }
    }

    public String save() {
        if (selectedTank != null) {
            // Already stored
            try {
                tankService.save(selectedTank, userSession.getSabiBackendToken());
                tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                e.printStackTrace();
                MessageUtil.warn("messages","common.error.internal_server_problem.t",userSession.getLocale());
            }
        }
        return TANK_VIEW_PAGE;
    }

}
