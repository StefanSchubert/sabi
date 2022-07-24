/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.PlagueService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Controller for the plague center as shown in plagueView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
@Getter
@Setter
public class PlagueView extends AbstractControllerTools implements Serializable  {

    private static final String PLAGUE_VIEW_PAGE = "plagueView";

    @Autowired
    TankService tankService;

    @Autowired
    PlagueService plagueService;

    @Inject
    UserSession userSession;

    private Long choosenTank;

    private PlagueRecordTo plagueRecordTo = new PlagueRecordTo();

    private List<AquariumTo> tanks;
    private List<PlagueTo> knownPlagues;

    private List<PlagueTo> ongoingUserPlagues;

    @PostConstruct
    public void init() {
        // user should be able to choose from his tanks
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            if (tanks.size() == 1) {
                // default selection if user has only one tank
                choosenTank = tanks.get(0).getId();
            }
        } catch (BusinessException e) {
            tanks = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg","common.token.expired.t",userSession.getLocale());
        }

        try {
            knownPlagues = plagueService.getPlagueCatalogue(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            knownPlagues = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg","common.token.expired.t",userSession.getLocale());
        }


    }


    /**
     * Used to request the TankName, when all you have is a reference Id.
     *
     * @param tankId technical key of the Tank.
     * @return "N/A" if tankId is unknown
     */
    @NotNull
    public String getTankNameForId(Long tankId) {
        return getTankNameForId(tankId,tanks);
    }


    public String save() {
        if (allDataProvided(plagueRecordTo)) {
            try {
                plagueService.save(plagueRecordTo, userSession.getSabiBackendToken());
                MessageUtil.info("submitState", "common.save.confirmation.t", userSession.getLocale());
            } catch (Exception e) {
                log.error("Couldn't save plague record {} {}",plagueRecordTo, e);
                MessageUtil.error("submitState", "common.error.internal_server_problem.t", userSession.getLocale());
            }
        } else {
            MessageUtil.warn("troubleMsg", "common.incompleted_formdata.t", userSession.getLocale());
        }
        return PLAGUE_VIEW_PAGE;
    }

    private boolean allDataProvided(PlagueRecordTo plagueRecordTo) {
        boolean result = true;
        if (plagueRecordTo.getObservedOn() == null) result = false;
        if (plagueRecordTo.getAquariumId() == null) result = false;
        if (plagueRecordTo.getPlagueStatusId() == 0) result = false;
        log.debug("allDataProvided = {}, Object was {}",result,plagueRecordTo);
        return result;
    }

    public Boolean getAreCurrentPlaguesReported (){
        return (ongoingUserPlagues == null || ongoingUserPlagues.isEmpty() ? true : false);
    }

}
