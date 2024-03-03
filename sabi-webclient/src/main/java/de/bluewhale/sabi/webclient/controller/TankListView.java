/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.WaterType;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.bluewhale.sabi.webclient.utils.PageRegister.TANK_EDITOR_PAGE;
import static de.bluewhale.sabi.webclient.utils.PageRegister.TANK_VIEW_PAGE;

/**
 * Controller for the Tanklist View as shown in tankView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Getter
public class TankListView implements Serializable {

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    private List<AquariumTo> tanks;
    private AquariumTo selectedTank;
    private Map<WaterType, String> translatedWaterType;

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

        translatedWaterType = new HashMap<WaterType, String>();
        for (WaterType value : WaterType.values()) {
            String msgKey = "enum.waterType."+value.getWaterType()+".l";
            String translatedType = MessageUtil.getFromMessageProperties(msgKey, userSession.getLocale());
            translatedWaterType.put(value,translatedType);
        }

    }

    @PreDestroy
    public void endOfService() {
        log.debug("Called predestroy on TankListView");
    }

    public String edit(AquariumTo tank) {
        selectedTank = tank;
        return TANK_EDITOR_PAGE.getNavigationableAddress();
    }

    public String addTank() {
        selectedTank = new AquariumTo();
        return TANK_EDITOR_PAGE.getNavigationableAddress();
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

    public void generateTemperatureApiKey(AquariumTo tank) {
        selectedTank = tank;
        try {
            String apiKey = tankService.reCreateTemperatureAPIKey(tank.getId(), userSession.getSabiBackendToken());
            tank.setTemperatueApiKey(apiKey);
        } catch (BusinessException e) {
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
        return TANK_VIEW_PAGE.getNavigationableAddress();
    }

}
