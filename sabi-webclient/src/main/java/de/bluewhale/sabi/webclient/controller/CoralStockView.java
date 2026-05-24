/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralStockEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSF CDI-Bean controller for the coral stock tab.
 * Manages the coral list split into active (no departedOn) and departed entries.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralStockView implements Serializable {

    @Autowired
    CoralStockService coralStockService;

    @Inject
    UserSession userSession;

    @Inject
    TankListView tankListView;

    @Inject
    CoralEntryNavContext coralEntryNavContext;

    /** Active corals (no departedOn). */
    private List<CoralStockEntryTo> activeCorals = new ArrayList<>();

    /** Departed corals (departedOn set). */
    private List<CoralStockEntryTo> departedCorals = new ArrayList<>();

    /** Currently selected coral for editing/departure. */
    private CoralStockEntryTo selectedCoral;

    /** Whether the departed-section is expanded (FR-007: default collapsed). */
    private boolean departedSectionExpanded = false;

    /** Selected aquarium ID from the tank-selector dropdown. */
    private Long selectedAquariumId;

    /** Whether the delete attempt was blocked by a departure record. */
    private boolean deleteBlocked = false;

    @PostConstruct
    public void init() {
        log.debug("CoralStockView init for user={}", userSession.getUserName());

        // Auto-preselect single aquarium
        if (selectedAquariumId == null
                && tankListView.getTanks() != null
                && tankListView.getTanks().size() == 1) {
            selectedAquariumId = tankListView.getTanks().get(0).getId();
            if (userSession.getSelectedTank() == null) {
                userSession.setSelectedTank(tankListView.getTanks().get(0));
            }
        }

        // Restore from session
        if (selectedAquariumId == null && userSession.getSelectedTank() != null) {
            selectedAquariumId = userSession.getSelectedTank().getId();
        }

        // Also restore from nav context
        if (selectedAquariumId == null && coralEntryNavContext.getSelectedAquariumId() != null) {
            selectedAquariumId = coralEntryNavContext.getSelectedAquariumId();
        }

        Long tankId = selectedAquariumId != null ? selectedAquariumId :
                (userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
        if (tankId == null) return;

        loadCoralsForTank(tankId);
    }

    public void onTankSelected() {
        if (selectedAquariumId == null) {
            activeCorals.clear();
            departedCorals.clear();
            return;
        }
        if (tankListView.getTanks() != null) {
            tankListView.getTanks().stream()
                    .filter(t -> selectedAquariumId.equals(t.getId()))
                    .findFirst()
                    .ifPresent(t -> userSession.setSelectedTank(t));
        }
        loadCoralsForTank(selectedAquariumId);
    }

    private void loadCoralsForTank(Long tankId) {
        try {
            List<CoralStockEntryTo> all = coralStockService.getCoralsForTank(
                    tankId, userSession.getSabiBackendToken());
            activeCorals = all.stream()
                    .filter(c -> c.getDepartedOn() == null)
                    .collect(Collectors.toList());
            departedCorals = all.stream()
                    .filter(c -> c.getDepartedOn() != null)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            log.error("Failed to load corals for tank {}", tankId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public String onAddCoral() {
        CoralStockEntryTo newEntry = new CoralStockEntryTo();
        newEntry.setAquariumId(selectedAquariumId);
        newEntry.setAddedOn(LocalDate.now());
        coralEntryNavContext.prepare(newEntry);
        return "/secured/coralStockEntryPage?faces-redirect=true";
    }

    public String onEditCoral(CoralStockEntryTo coral) {
        coralEntryNavContext.prepare(coral);
        return "/secured/coralStockEntryPage?faces-redirect=true";
    }

    public void onDeleteCoral(CoralStockEntryTo coral) {
        deleteBlocked = false;
        try {
            coralStockService.deleteCoral(coral.getId(), userSession.getSabiBackendToken());
            activeCorals.remove(coral);
            departedCorals.remove(coral);
        } catch (BusinessException e) {
            log.warn("Could not delete coral {}: {}", coral.getId(), e.getMessage());
            deleteBlocked = true;
            MessageUtil.error(null, "coralstock.delete.denied.label", userSession.getLocale());
        }
    }

    public String onRecordDeparture(CoralStockEntryTo coral) {
        coralEntryNavContext.prepare(coral);
        return "/secured/coralDepartureForm?faces-redirect=true";
    }


    public void onRemoveCatalogueLink(CoralStockEntryTo coral) {
        try {
            coralStockService.removeCatalogueLink(coral.getId(), userSession.getSabiBackendToken());
            coral.setCoralCatalogueId(null);
        } catch (BusinessException e) {
            log.error("Failed to remove catalogue link for coral {}", coral.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}

