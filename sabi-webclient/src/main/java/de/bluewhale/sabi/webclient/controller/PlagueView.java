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
import de.bluewhale.sabi.webclient.model.ActivePlagueTo;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
public class PlagueView extends AbstractControllerTools implements Serializable {

    private static final String PLAGUE_VIEW_PAGE = "plagueView";
    private static final Integer PLAGUE_CURED_STATUS_ID = 5; // Needs to match vanished state in table localized_plague_status.

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

    private List<ActivePlagueTo> ongoingUserPlagues;

    private List<PlagueTo> pastUserPlagues;

    @PostConstruct
    public void init() {

        // Know Tanks of user
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            if (tanks.size() == 1) {
                // default selection if user has only one tank
                choosenTank = tanks.get(0).getId();
            }
        } catch (BusinessException e) {
            tanks = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }

        // Fetch Plague Catalogue
        try {
            knownPlagues = plagueService.getPlagueCatalogue(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            knownPlagues = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }

        // Get Plagues reported by user
        List<PlagueRecordTo> activePlaguesOfUsersTanks = null;
        try {
            activePlaguesOfUsersTanks = plagueService.getPlagueRecordsForUserTanks(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }
        if (activePlaguesOfUsersTanks == null) {
            activePlaguesOfUsersTanks = Collections.emptyList();
        }

        // Determine current active plagues for each tank of the user
        for (AquariumTo tank : tanks) {

            // Slice for current tank;plagueInterval
            List<PlagueRecordTo> sortedActivePlaguesOfUsersTank = activePlaguesOfUsersTanks.stream()
                    .filter(item -> item.getAquariumId() == tank.getId())
                    .sorted(Comparator.comparingInt(PlagueRecordTo::getPlagueIntervallId))
                    .collect(Collectors.toList());


            PlagueRecordTo lastPlagueRecord = null;

            for (PlagueRecordTo plagueRecord : sortedActivePlaguesOfUsersTank) {

                // init case
                if (lastPlagueRecord == null) {
                    lastPlagueRecord = plagueRecord;
                }

                if (lastPlagueRecord.getPlagueIntervallId() == plagueRecord.getPlagueIntervallId()) {
                    // Same intervall, swap lastPlagueRecord if newer observed date
                    if (lastPlagueRecord.getObservedOn().isBefore(plagueRecord.getObservedOn())) {
                        lastPlagueRecord = plagueRecord;
                    }
                } else {
                    // different intervall, store last entry if state is not a closed one
                    if (lastPlagueRecord.getPlagueStatusId() != PLAGUE_CURED_STATUS_ID) {
                        addOngoingPlague(tank, lastPlagueRecord);
                        lastPlagueRecord = plagueRecord;
                    }
                }
            }
            // Handle last Intervall
            if (!sortedActivePlaguesOfUsersTank.isEmpty() && (lastPlagueRecord.getPlagueStatusId() != PLAGUE_CURED_STATUS_ID)) {
                addOngoingPlague(tank, lastPlagueRecord);
            }
        }

    }

    private void addOngoingPlague(AquariumTo pTank, PlagueRecordTo pLastPlagueRecord) {
        ActivePlagueTo activePlagueTo = new ActivePlagueTo();
        activePlagueTo.setTankName(pTank.getDescription());
        activePlagueTo.setObservedOn(pLastPlagueRecord.getObservedOn());
        // FIXME STS (30.09.22): get Localized String for PlagueStatus
        activePlagueTo.setCurrentStatus("" + pLastPlagueRecord.getPlagueStatusId());
        // FIXME STS (30.09.22): get Localized String for PlageName
        activePlagueTo.setPlageName("" + pLastPlagueRecord.getPlagueId());
        ongoingUserPlagues.add(activePlagueTo);
    }


    /**
     * Used to request the TankName, when all you have is a reference Id.
     *
     * @param tankId technical key of the Tank.
     * @return "N/A" if tankId is unknown
     */
    @NotNull
    public String getTankNameForId(Long tankId) {
        return getTankNameForId(tankId, tanks);
    }


    public String save() {
        if (allDataProvided(plagueRecordTo)) {
            try {
                plagueService.save(plagueRecordTo, userSession.getSabiBackendToken());
                MessageUtil.info("submitState", "common.save.confirmation.t", userSession.getLocale());
            } catch (Exception e) {
                log.error("Couldn't save plague record {} {}", plagueRecordTo, e);
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
        log.debug("allDataProvided = {}, Object was {}", result, plagueRecordTo);
        return result;
    }

    public Boolean getAreCurrentPlaguesReported() {
        return (ongoingUserPlagues == null || ongoingUserPlagues.isEmpty() ? true : false);
    }

    public Boolean getExistsRecordsOnPastPlagues() {
        return (pastUserPlagues == null || pastUserPlagues.isEmpty() ? true : false);
    }


    public void fetchAnnouncement() {
        String plagueInfo = MessageUtil.getFromMessageProperties("plague.center.info.t", LocaleContextHolder.getLocale());
        MessageUtil.info("plaguemessages", plagueInfo);
    }

    public String getAnnouncement() {
        // This is a little hack as using the viewAction from Metadata doesn't seem to work.
        fetchAnnouncement();
        return "";
    }

}
