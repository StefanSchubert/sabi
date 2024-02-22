/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.PlagueService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.model.PastPlagueTo;
import de.bluewhale.sabi.webclient.model.ReportedPlagueTo;
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
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static de.bluewhale.sabi.webclient.utils.PageRegister.PLAGUE_VIEW_PAGE;

/**
 * Controller for the plague center as shown in plagueView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class PlagueView extends AbstractControllerTools implements Serializable {

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

    private List<PlagueStatusTo> plagueStatusToList;

    private List<PlagueRecordTo> plaguesOfUsersTanks;
    private List<ReportedPlagueTo> ongoingUserPlagues;

    private List<PastPlagueTo> pastUserPlagues;

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

        // Fetch i18n Plague Catalogue
        try {
            knownPlagues = plagueService.getPlagueCatalogue(userSession.getSabiBackendToken(), userSession.getLanguage());
        } catch (BusinessException e) {
            knownPlagues = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }

        // Fetch 18n Plague Status List
        try {
            plagueStatusToList = plagueService.getPlagueStatusList(userSession.getSabiBackendToken(), userSession.getLanguage());
        } catch (BusinessException e) {
            plagueStatusToList = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }

        // Get Plagues reported by user
        plaguesOfUsersTanks = null;
        try {
            List<PlagueRecordTo> unmutableList = plagueService.getPlagueRecordsForUserTanks(userSession.getSabiBackendToken());
            plaguesOfUsersTanks = new ArrayList<>(unmutableList);
        } catch (BusinessException e) {
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg", "common.token.expired.t", userSession.getLocale());
        }
        if (plaguesOfUsersTanks == null) {
            plaguesOfUsersTanks = Collections.emptyList();
        }

        // Determine solved plagues for user tanks
        pastUserPlagues = new ArrayList<>();
        List<PlagueRecordTo> curedPlaguesOfUsersTank = plaguesOfUsersTanks.stream()
                .filter(item -> item.getPlagueStatusId() == PLAGUE_CURED_STATUS_ID)
                .sorted(Comparator.comparingLong(PlagueRecordTo::getAquariumId))
                .collect(Collectors.toList());

        for (PlagueRecordTo recordTo : curedPlaguesOfUsersTank) {
            PastPlagueTo pastPlagueTo = convertToPastPlagueTo(recordTo, getTankNameForId(recordTo.getAquariumId()));
            Duration duration = calcPlaguesDuration(pastPlagueTo,plaguesOfUsersTanks);
            pastPlagueTo.setDuration(duration.toDays());
            pastUserPlagues.add(pastPlagueTo);
        }

        // Determine current active plagues for each tank of the user
        for (AquariumTo tank : tanks) {

            // Slice for current tank;plagueInterval
            List<PlagueRecordTo> sortedActivePlaguesOfUsersTank = plaguesOfUsersTanks.stream()
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
                    }
                    lastPlagueRecord = plagueRecord;
                }
            }
            // Handle last Intervall
            if (!sortedActivePlaguesOfUsersTank.isEmpty() && (lastPlagueRecord.getPlagueStatusId() != PLAGUE_CURED_STATUS_ID)) {
                addOngoingPlague(tank, lastPlagueRecord);
            }
        }
    }

    private Duration calcPlaguesDuration(PastPlagueTo closingPlagueIntevalTo, List<PlagueRecordTo> allPlaguesOfUsersTanks) {
        // pastPlagueTo contains already the end observation, so the close date of the interval
        // we require to find the first observation of the interval to calc duration between both
        PlagueRecordTo firstObservedPlagueRecordTo = allPlaguesOfUsersTanks.stream().filter(plage -> plage.getPlagueIntervallId().equals(closingPlagueIntevalTo.getPlagueIntervallId())).min(Comparator.comparing(PlagueRecordTo::getObservedOn)).get();
        Duration duration = Duration.between(firstObservedPlagueRecordTo.getObservedOn(), closingPlagueIntevalTo.getObservedOn());
        return duration;
    }

    private void addOngoingPlague(AquariumTo pTank, PlagueRecordTo pLastPlagueRecord) {

        if (ongoingUserPlagues == null) {
            ongoingUserPlagues = new ArrayList<>();
        }
        String tankName = pTank.getDescription();

        ReportedPlagueTo reportedPlagueTo = convertToReportedPlagueTo(pLastPlagueRecord, tankName);
        ongoingUserPlagues.add(reportedPlagueTo);
    }

    private ReportedPlagueTo convertToReportedPlagueTo(PlagueRecordTo pLastPlagueRecord, String tankName) {
        ReportedPlagueTo reportedPlagueTo = new ReportedPlagueTo();
        reportedPlagueTo.setTankName(tankName);
        reportedPlagueTo.setObservedOn(pLastPlagueRecord.getObservedOn());
        reportedPlagueTo.setCurrentStatus(getPlagueStatusDescriptionFor(pLastPlagueRecord.getPlagueStatusId()));
        reportedPlagueTo.setPlageName(getCommonPlagueNameFor(pLastPlagueRecord.getPlagueId()));
        reportedPlagueTo.setPlagueIntervallId(pLastPlagueRecord.getPlagueIntervallId());
        return reportedPlagueTo;
    }

    private PastPlagueTo convertToPastPlagueTo(PlagueRecordTo pLastPlagueRecord, String tankName) {
        PastPlagueTo pastPlagueTo = new PastPlagueTo();
        pastPlagueTo.setTankName(tankName);
        pastPlagueTo.setObservedOn(pLastPlagueRecord.getObservedOn());
        pastPlagueTo.setCurrentStatus(getPlagueStatusDescriptionFor(pLastPlagueRecord.getPlagueStatusId()));
        pastPlagueTo.setPlageName(getCommonPlagueNameFor(pLastPlagueRecord.getPlagueId()));
        pastPlagueTo.setPlagueIntervallId(pLastPlagueRecord.getPlagueIntervallId());
        return pastPlagueTo;
    }


    private String getCommonPlagueNameFor(Integer pPlagueId) {
        String plagueName;
        Optional<PlagueTo> plagueTo = knownPlagues.stream().filter(item -> item.getId() == pPlagueId).findFirst();
        plagueName = (plagueTo.isPresent() ? plagueTo.get().getCommonName() : "?");
        return plagueName;
    }

    private String getPlagueStatusDescriptionFor(Integer pPlagueStatusId) {
        String plagueStatus;
        Optional<PlagueStatusTo> statusTo = plagueStatusToList.stream().filter(item -> item.getId() == pPlagueStatusId).findFirst();
        plagueStatus = (statusTo.isPresent() ? statusTo.get().getDescription() : "?");
        return plagueStatus;
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

            if (ongoingUserPlagues == null) ongoingUserPlagues = new ArrayList<>();

            // Determine IntervallId by looking into ongoing list
            Integer intervallId;
            List<ReportedPlagueTo> matchList = ongoingUserPlagues.stream()
                    .filter(item -> item.getPlageName().equalsIgnoreCase(getCommonPlagueNameFor(plagueRecordTo.getPlagueId())))
                    .collect(Collectors.toList());

            if (matchList.isEmpty()) {
                // no match in ongoing list => new plague intervall occurrence, determine new intervallId by looking on all records
                Optional<PlagueRecordTo> optMaxIntervalId = plaguesOfUsersTanks.stream().max(Comparator.comparingInt(PlagueRecordTo::getPlagueIntervallId));
                if (optMaxIntervalId.isEmpty()) {
                    intervallId = 1;
                } else {
                    intervallId = optMaxIntervalId.get().getPlagueIntervallId() + 1;
                }
            } else {
                // plage record complements ongoing plague
                intervallId = matchList.get(0).getPlagueIntervallId();
            }

            plagueRecordTo.setPlagueIntervallId(intervallId);

            try {
                plagueService.save(plagueRecordTo, userSession.getSabiBackendToken());
                MessageUtil.info("saveresult", "common.save.confirmation.t", userSession.getLocale());

                plaguesOfUsersTanks.add(plagueRecordTo);
                String tankName = getTankNameForId(plagueRecordTo.getAquariumId());
                if (plagueRecordTo.getPlagueStatusId() == PLAGUE_CURED_STATUS_ID) {
                    PastPlagueTo pastPlagueTo = convertToPastPlagueTo(plagueRecordTo, tankName);
                    Duration duration = calcPlaguesDuration(pastPlagueTo,plaguesOfUsersTanks);
                    pastPlagueTo.setDuration(duration.toDays());
                    pastUserPlagues.add(pastPlagueTo);
                    // and remove occurrences of the intervall in ongoing list, if any
                    List<ReportedPlagueTo> removeCandidates = ongoingUserPlagues.stream().filter(item -> item.getPlageName().equalsIgnoreCase(getCommonPlagueNameFor(plagueRecordTo.getPlagueId())) &&
                            item.getPlagueIntervallId() == plagueRecordTo.getPlagueIntervallId()).collect(Collectors.toList());
                    ongoingUserPlagues.removeAll(removeCandidates);

                } else {
                    List<ReportedPlagueTo> collect = ongoingUserPlagues.stream().filter(item -> item.getPlageName().equalsIgnoreCase(getCommonPlagueNameFor(plagueRecordTo.getPlagueId()))).collect(Collectors.toList());
                    if (collect.isEmpty()) {
                        ongoingUserPlagues.add(convertToReportedPlagueTo(plagueRecordTo, tankName));
                    } else {
                        // check on newer Observation Date, if it should be added as last recent state
                        ReportedPlagueTo recentOngoingEntry = collect.get(0);
                        if (recentOngoingEntry.getObservedOn().isBefore(plagueRecordTo.getObservedOn())) {
                            ongoingUserPlagues.add(convertToReportedPlagueTo(plagueRecordTo, tankName));
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Couldn't save plague record {} {}", plagueRecordTo, e);
                e.printStackTrace();
                MessageUtil.error("saveresult", "common.error.internal_server_problem.t", userSession.getLocale());
            }
        } else {
            MessageUtil.warn("saveresult", "common.incompleted_formdata.t", userSession.getLocale());
        }
        return PLAGUE_VIEW_PAGE.getNavigationableAddress();
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
        return (ongoingUserPlagues != null && (ongoingUserPlagues.isEmpty() ? false : true));
    }

    public Boolean getExistsRecordsOnPastPlagues() {
        return (pastUserPlagues != null || (pastUserPlagues.isEmpty() ? false : true));
    }

    public void fetchAnnouncement() {
        String plagueInfo = MessageUtil.getFromMessageProperties("plague.center.info.t", LocaleContextHolder.getLocale());
        MessageUtil.info("common", plagueInfo);
    }

    public String getAnnouncement() {
        // This is a little hack as using the viewAction from Metadata doesn't seem to work.
        fetchAnnouncement();
        return "";
    }

}
