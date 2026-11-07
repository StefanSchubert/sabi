/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.DosingSuggestionsTo;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.DosingType;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.DosingService;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for the standalone dosing records page.
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class DosingView extends AbstractControllerTools implements Serializable {

    private static final String SELECTED_AQUARIUM_VIEW_KEY = DosingView.class.getName() + ".selectedAquariumId";

    @Autowired
    DosingService dosingService;

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    private List<AquariumTo> tanks = Collections.emptyList();
    private List<DosingTo> dosings = Collections.emptyList();
    @Setter(AccessLevel.NONE)
    private Long selectedAquariumId;
    private DosingTo dosing = newDosing();
    private DosingSuggestionsTo suggestions = new DosingSuggestionsTo();

    @PostConstruct
    public void init() {
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            selectedAquariumId = (Long) FacesContext.getCurrentInstance().getViewRoot().getViewMap()
                    .get(SELECTED_AQUARIUM_VIEW_KEY);
            if (selectedAquariumId == null) {
                // Cross-link from other views (e.g. the Maintenance page): accept a validated
                // aquariumId request parameter, but only for tanks that belong to this user.
                selectedAquariumId = resolveOwnedTankFromRequestParameter();
            }
            if (selectedAquariumId == null) {
                // Re-entry from the menu: restore the tank the user last selected (spec 007,
                // issue "re-entry shows empty lists"). The ID is validated against the user's tanks.
                selectedAquariumId = resolveOwnedTankFromSession();
            }
            if (selectedAquariumId == null) {
                // Fresh session (e.g. after re-login): fall back to the first tank (lowest ID)
                // so that existing data is shown instead of an empty placeholder.
                setSelectedAquariumId(firstTankId());
            }
            if (selectedAquariumId != null) {
                loadDosings();
            }
            loadSuggestions();
        } catch (BusinessException e) {
            log.error("Could not load tanks for dosing page", e);
            MessageUtil.error("dosingMessages", "common.token.expired.t", userSession.getLocale());
        }
    }

    private Long resolveOwnedTankFromRequestParameter() {
        String parameter = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
                .get("aquariumId");
        if (parameter == null || parameter.isBlank()) {
            return null;
        }
        try {
            Long parameterId = Long.parseLong(parameter);
            for (AquariumTo tank : tanks) {
                if (tank.getId().equals(parameterId)) {
                    return parameterId;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Ignored malformed aquariumId request parameter on dosing page");
        }
        return null;
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

    /**
     * Deterministic fallback selection: the tank with the lowest ID. The backend returns tanks
     * without a guaranteed order, so "first in list" would be non-deterministic.
     */
    private Long firstTankId() {
        return tanks.stream()
                .min(Comparator.comparing(AquariumTo::getId))
                .map(AquariumTo::getId)
                .orElse(null);
    }

    public void onAquariumChange() {
        resetForm();
        loadDosings();
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

    public void save() {
        if (!isComplete()) {
            MessageUtil.warn("dosingMessages", "common.incompleted_formdata.t", userSession.getLocale());
            return;
        }
        if (dosing.getDosingType() == DosingType.MANUAL_ADDITION) {
            dosing.setDosingInterval(null);
            dosing.setDosingEndOn(null);
        }
        dosing.setAquariumId(selectedAquariumId);
        try {
            if (dosing.getId() == null) {
                dosingService.createDosing(selectedAquariumId, dosing, userSession.getSabiBackendToken());
            } else {
                dosingService.updateDosing(selectedAquariumId, dosing.getId(), dosing, userSession.getSabiBackendToken());
            }
            resetForm();
            loadDosings();
            // Refresh the autocomplete suggestions so that values just saved are offered
            // immediately without a page reload.
            loadSuggestions();
            MessageUtil.info("dosingMessages", "common.save.confirmation.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save dosing for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("dosingMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void edit(DosingTo existingDosing) {
        dosing = existingDosing;
        setSelectedAquariumId(existingDosing.getAquariumId());
    }

    public void delete(DosingTo existingDosing) {
        try {
            dosingService.deleteDosing(existingDosing.getAquariumId(), existingDosing.getId(),
                    userSession.getSabiBackendToken());
            if (existingDosing.getId().equals(dosing.getId())) {
                resetForm();
            }
            loadDosings();
            MessageUtil.info("dosingMessages", "dosing.delete.success.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not delete dosing_id={} for aquarium_id={}", existingDosing.getId(),
                    existingDosing.getAquariumId(), e);
            MessageUtil.error("dosingMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    public void resetForm() {
        dosing = newDosing();
    }

    public String getDosingTypeLabel(DosingType dosingType) {
        return switch (dosingType) {
            case MANUAL_ADDITION -> MessageUtil.getFromMessageProperties("dosing.type.manual_addition.l",
                    userSession.getLocale());
            case AUTOMATED_DOSING -> MessageUtil.getFromMessageProperties("dosing.type.automated_dosing.l",
                    userSession.getLocale());
        };
    }

    public DosingType[] getDosingTypes() {
        return DosingType.values();
    }

    private void loadDosings() {
        if (selectedAquariumId == null) {
            dosings = Collections.emptyList();
            return;
        }
        try {
            dosings = dosingService.listDosingsForTank(selectedAquariumId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            dosings = Collections.emptyList();
            log.error("Could not load dosings for aquarium_id={}", selectedAquariumId, e);
            MessageUtil.error("dosingMessages", "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    private void loadSuggestions() {
        // Non-critical: a failure only means the form has no autocomplete suggestions.
        try {
            suggestions = dosingService.getSuggestions(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            suggestions = new DosingSuggestionsTo();
            log.warn("Could not load dosing suggestions", e);
        }
    }

    public List<String> productSuggestions(String query) {
        return filterSuggestions(suggestions.getProductNames(), query);
    }

    public List<String> categorySuggestions(String query) {
        return filterSuggestions(suggestions.getCategories(), query);
    }

    public List<String> methodSuggestions(String query) {
        return filterSuggestions(suggestions.getDosingMethods(), query);
    }

    private List<String> filterSuggestions(List<String> values, String query) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return values;
        }
        String lowerQuery = query.toLowerCase();
        return values.stream()
                .filter(value -> value.toLowerCase().contains(lowerQuery))
                .toList();
    }

    private boolean isComplete() {
        if (selectedAquariumId == null || dosing.getRecordedOn() == null || dosing.getDosingType() == null
                || dosing.getProductName() == null || dosing.getProductName().isBlank() || dosing.getAmount() == null
                || dosing.getAmount().signum() <= 0 || dosing.getAmountUnit() == null || dosing.getAmountUnit().isBlank()) {
            return false;
        }
        return dosing.getDosingType() != DosingType.AUTOMATED_DOSING
                || (dosing.getDosingInterval() != null && !dosing.getDosingInterval().isBlank());
    }

    private DosingTo newDosing() {
        DosingTo newDosing = new DosingTo();
        newDosing.setRecordedOn(LocalDateTime.now());
        return newDosing;
    }
}
