/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.CarbonMode;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceEventType;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MaintenanceService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Controller for the per-aquarium maintenance page: regular water exchange interval, recorded
 * maintenance events (water changes and carbon replacements) and the activated carbon setting.
 * The selected tank is persisted in the session so that re-entering the page restores it.
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class MaintenanceView extends AbstractControllerTools implements Serializable {

    private static final String SELECTED_AQUARIUM_VIEW_KEY = MaintenanceView.class.getName() + ".selectedAquariumId";

    @Autowired
    MaintenanceService maintenanceService;

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    private List<AquariumTo> tanks = Collections.emptyList();
    @Setter(AccessLevel.NONE)
    private Long selectedAquariumId;
    private MaintenanceProfileTo profile = new MaintenanceProfileTo();
    private List<MaintenanceEventTo> waterChanges = Collections.emptyList();
    private List<MaintenanceEventTo> carbonReplacements = Collections.emptyList();
    private MaintenanceEventTo waterChange = newWaterChange();
    private MaintenanceEventTo carbonReplacement = newCarbonReplacement();

    @PostConstruct
    public void init() {
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            selectedAquariumId = (Long) FacesContext.getCurrentInstance().getViewRoot().getViewMap()
                    .get(SELECTED_AQUARIUM_VIEW_KEY);
            if (selectedAquariumId == null) {
                // Re-entry from the menu: restore the tank the user last selected (spec 007,
                // issue "re-entry shows empty lists"). The ID is validated against the user's tanks.
                selectedAquariumId = resolveOwnedTankFromSession();
            }
            if (selectedAquariumId == null && tanks.size() == 1) {
                setSelectedAquariumId(tanks.get(0).getId());
            }
            if (selectedAquariumId != null) {
                loadMaintenance();
            }
        } catch (BusinessException e) {
            log.error("Could not load tanks for maintenance page", e);
            MessageUtil.error("maintenanceMessages", "common.token.expired.t", userSession.getLocale());
        }
    }

    private Long resolveOwnedTankFromSession() {
        Long lastSelected = userSession.getLastSelectedTankId();
        if (lastSelected == null) {
            return null;
        }
        for (AquariumTo tank : tanks) {
            if (tank.getId().equals(lastSelected)) {
                return lastSelected;
            }
        }
        // Stale value (tank deleted or no longer owned) — ignore it.
        return null;
    }

    public void onAquariumChange() {
        resetWaterChangeForm();
        resetCarbonReplacementForm();
        loadMaintenance();
    }

    public void setSelectedAquariumId(Long selectedAquariumId) {
        this.selectedAquariumId = selectedAquariumId;
        if (selectedAquariumId == null) {
            FacesContext.getCurrentInstance().getViewRoot().getViewMap().remove(SELECTED_AQUARIUM_VIEW_KEY);
            userSession.setLastSelectedTankId(null);
        } else {
            FacesContext.getCurrentInstance().getViewRoot().getViewMap()
                    .put(SELECTED_AQUARIUM_VIEW_KEY, selectedAquariumId);
            userSession.setLastSelectedTankId(selectedAquariumId);
        }
    }

    public void saveInterval() {
        if (profile.getRegularIntervalDays() == null && profile.getRegularPercentage() != null) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        if (profile.getRegularIntervalDays() != null && profile.getRegularPercentage() == null) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        WaterChangeIntervalTo interval = new WaterChangeIntervalTo();
        interval.setIntervalDays(profile.getRegularIntervalDays());
        interval.setPercentage(profile.getRegularPercentage());
        try {
            profile = maintenanceService.updateWaterChangeInterval(selectedAquariumId, interval,
                    userSession.getSabiBackendToken());
            MessageUtil.info("maintenanceMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save water change interval for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void clearInterval() {
        profile.setRegularIntervalDays(null);
        profile.setRegularPercentage(null);
        WaterChangeIntervalTo interval = new WaterChangeIntervalTo();
        try {
            profile = maintenanceService.updateWaterChangeInterval(selectedAquariumId, interval,
                    userSession.getSabiBackendToken());
            MessageUtil.info("maintenanceMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not clear water change interval for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void saveCarbon() {
        if (Boolean.TRUE.equals(profile.getCarbonEnabled())) {
            if (profile.getCarbonMode() == null) {
                MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
                return;
            }
            if (profile.getCarbonMode() == CarbonMode.INTERVAL && profile.getCarbonReplacementIntervalDays() == null) {
                MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
                return;
            }
        }
        ActivatedCarbonTo carbon = new ActivatedCarbonTo();
        carbon.setEnabled(profile.getCarbonEnabled());
        if (Boolean.TRUE.equals(profile.getCarbonEnabled())) {
            carbon.setMode(profile.getCarbonMode());
            if (profile.getCarbonMode() == CarbonMode.INTERVAL) {
                carbon.setReplacementIntervalDays(profile.getCarbonReplacementIntervalDays());
            }
        }
        try {
            profile = maintenanceService.updateActivatedCarbon(selectedAquariumId, carbon,
                    userSession.getSabiBackendToken());
            MessageUtil.info("maintenanceMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save activated carbon setting for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void saveWaterChange() {
        if (waterChange.getChangedOn() == null || waterChange.getPercentage() == null) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        if (waterChange.getChangedOn().isAfter(LocalDate.now())) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        waterChange.setMaintenanceType(MaintenanceEventType.WATER_CHANGE);
        try {
            if (waterChange.getId() == null) {
                maintenanceService.createMaintenanceEvent(selectedAquariumId, waterChange, userSession.getSabiBackendToken());
            } else {
                maintenanceService.updateMaintenanceEvent(selectedAquariumId, waterChange.getId(), waterChange,
                        userSession.getSabiBackendToken());
            }
            resetWaterChangeForm();
            loadMaintenance();
            MessageUtil.info("maintenanceMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save water change for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void editWaterChange(MaintenanceEventTo existing) {
        waterChange = existing;
    }

    public void deleteWaterChange(MaintenanceEventTo existing) {
        deleteEvent(existing, "maintenance.delete.success.t");
    }

    public void saveCarbonReplacement() {
        if (carbonReplacement.getChangedOn() == null) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        if (carbonReplacement.getChangedOn().isAfter(LocalDate.now())) {
            MessageUtil.warn("maintenanceMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        carbonReplacement.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        carbonReplacement.setPercentage(null); // carbon replacements never carry a percentage
        try {
            if (carbonReplacement.getId() == null) {
                maintenanceService.createMaintenanceEvent(selectedAquariumId, carbonReplacement, userSession.getSabiBackendToken());
            } else {
                maintenanceService.updateMaintenanceEvent(selectedAquariumId, carbonReplacement.getId(), carbonReplacement,
                        userSession.getSabiBackendToken());
            }
            resetCarbonReplacementForm();
            loadMaintenance();
            MessageUtil.info("maintenanceMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save carbon replacement for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void editCarbonReplacement(MaintenanceEventTo existing) {
        carbonReplacement = existing;
    }

    public void deleteCarbonReplacement(MaintenanceEventTo existing) {
        deleteEvent(existing, "maintenance.delete.carbon.success.t");
    }

    private void deleteEvent(MaintenanceEventTo existing, String successMessageKey) {
        try {
            maintenanceService.deleteMaintenanceEvent(existing.getAquariumId(), existing.getId(),
                    userSession.getSabiBackendToken());
            if (existing.getId().equals(waterChange.getId())) {
                resetWaterChangeForm();
            }
            if (existing.getId().equals(carbonReplacement.getId())) {
                resetCarbonReplacementForm();
            }
            loadMaintenance();
            MessageUtil.info("maintenanceMessages", successMessageKey, userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not delete maintenance event_id={} for aquarium_id={}", existing.getId(),
                    existing.getAquariumId(), e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void resetWaterChangeForm() {
        waterChange = newWaterChange();
    }

    public void resetCarbonReplacementForm() {
        carbonReplacement = newCarbonReplacement();
    }

    public CarbonMode[] getCarbonModes() {
        return CarbonMode.values();
    }

    /**
     * Today as picker pattern string, used as maxDate for the event date pickers.
     */
    public String getToday() {
        return LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public String getCarbonModeLabel(CarbonMode mode) {
        return switch (mode) {
            case CONTINUOUS -> MessageUtil.getFromMessageProperties("maintenance.carbon.mode.continuous.l",
                    userSession.getLocale());
            case INTERVAL -> MessageUtil.getFromMessageProperties("maintenance.carbon.mode.interval.l",
                    userSession.getLocale());
        };
    }

    private void loadMaintenance() {
        if (selectedAquariumId == null) {
            profile = new MaintenanceProfileTo();
            waterChanges = Collections.emptyList();
            carbonReplacements = Collections.emptyList();
            return;
        }
        try {
            var maintenance = maintenanceService.getMaintenanceForTank(selectedAquariumId, userSession.getSabiBackendToken());
            profile = maintenance.getProfile() != null ? maintenance.getProfile() : new MaintenanceProfileTo();
            List<MaintenanceEventTo> events =
                    maintenance.getEvents() != null ? maintenance.getEvents() : Collections.emptyList();
            waterChanges = events.stream()
                    .filter(event -> event.getMaintenanceType() == MaintenanceEventType.WATER_CHANGE)
                    .toList();
            carbonReplacements = events.stream()
                    .filter(event -> event.getMaintenanceType() == MaintenanceEventType.CARBON_REPLACEMENT)
                    .toList();
        } catch (BusinessException e) {
            profile = new MaintenanceProfileTo();
            waterChanges = Collections.emptyList();
            carbonReplacements = Collections.emptyList();
            log.error("Could not load maintenance for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("maintenanceMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    private MaintenanceEventTo newWaterChange() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.WATER_CHANGE);
        to.setChangedOn(LocalDate.now());
        return to;
    }

    private MaintenanceEventTo newCarbonReplacement() {
        MaintenanceEventTo to = new MaintenanceEventTo();
        to.setMaintenanceType(MaintenanceEventType.CARBON_REPLACEMENT);
        to.setChangedOn(LocalDate.now());
        return to;
    }
}
