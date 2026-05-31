/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.InvertebrateStockEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateStockService;
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
 * JSF CDI-Bean controller for the invertebrate stock tab.
 * Manages the invertebrate list split into active (no departedOn) and departed entries.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class InvertebrateStockView implements Serializable {

    @Autowired
    InvertebrateStockService invertebrateStockService;

    @Inject
    UserSession userSession;

    @Inject
    TankListView tankListView;

    @Inject
    InvertebrateEntryNavContext invertebrateEntryNavContext;

    /** Active invertebrates (no departedOn). */
    private List<InvertebrateStockEntryTo> activeInvertebrates = new ArrayList<>();

    /** Departed invertebrates (departedOn set). */
    private List<InvertebrateStockEntryTo> departedInvertebrates = new ArrayList<>();

    /** Currently selected invertebrate for editing/departure. */
    private InvertebrateStockEntryTo selectedInvertebrate;

    /** Whether the departed-section is expanded (default collapsed). */
    private boolean departedSectionExpanded = false;

    /** Selected aquarium ID from the tank-selector dropdown. */
    private Long selectedAquariumId;

    /** Whether the delete attempt was blocked by a departure record. */
    private boolean deleteBlocked = false;

    @PostConstruct
    public void init() {
        log.debug("InvertebrateStockView init for user={}", userSession.getUserName());

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
        if (selectedAquariumId == null && invertebrateEntryNavContext.getSelectedAquariumId() != null) {
            selectedAquariumId = invertebrateEntryNavContext.getSelectedAquariumId();
        }

        Long tankId = selectedAquariumId != null ? selectedAquariumId :
                (userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
        if (tankId == null) return;

        loadInvertebratesForTank(tankId);
    }

    public void onTankSelected() {
        if (selectedAquariumId == null) {
            activeInvertebrates.clear();
            departedInvertebrates.clear();
            return;
        }
        if (tankListView.getTanks() != null) {
            tankListView.getTanks().stream()
                    .filter(t -> selectedAquariumId.equals(t.getId()))
                    .findFirst()
                    .ifPresent(t -> userSession.setSelectedTank(t));
        }
        loadInvertebratesForTank(selectedAquariumId);
    }

    private void loadInvertebratesForTank(Long tankId) {
        try {
            List<InvertebrateStockEntryTo> all = invertebrateStockService.getInvertebratesForTank(
                    tankId, userSession.getSabiBackendToken());
            activeInvertebrates = all.stream()
                    .filter(i -> i.getDepartedOn() == null)
                    .collect(Collectors.toList());
            departedInvertebrates = all.stream()
                    .filter(i -> i.getDepartedOn() != null)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            log.error("Failed to load invertebrates for tank {}", tankId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public String onAddInvertebrate() {
        InvertebrateStockEntryTo newEntry = new InvertebrateStockEntryTo();
        newEntry.setAquariumId(selectedAquariumId);
        newEntry.setAddedOn(LocalDate.now());
        invertebrateEntryNavContext.prepare(newEntry);
        return "/secured/invertebrateStockEntryPage?faces-redirect=true";
    }

    public String onEditInvertebrate(InvertebrateStockEntryTo invertebrate) {
        invertebrateEntryNavContext.prepare(invertebrate);
        return "/secured/invertebrateStockEntryPage?faces-redirect=true";
    }

    public void onDeleteInvertebrate(InvertebrateStockEntryTo invertebrate) {
        deleteBlocked = false;
        try {
            invertebrateStockService.deleteInvertebrate(invertebrate.getId(), userSession.getSabiBackendToken());
            activeInvertebrates.remove(invertebrate);
            departedInvertebrates.remove(invertebrate);
        } catch (BusinessException e) {
            log.warn("Could not delete invertebrate {}: {}", invertebrate.getId(), e.getMessage());
            deleteBlocked = true;
            MessageUtil.error(null, "invertebratestock.delete.denied.label", userSession.getLocale());
        }
    }

    public String onRecordDeparture(InvertebrateStockEntryTo invertebrate) {
        invertebrateEntryNavContext.prepare(invertebrate);
        return "/secured/invertebrateDepartureForm?faces-redirect=true";
    }

    public void onRemoveCatalogueLink(InvertebrateStockEntryTo invertebrate) {
        try {
            invertebrateStockService.removeCatalogueLink(invertebrate.getId(), userSession.getSabiBackendToken());
            invertebrate.setInvertebrateCatalogueId(null);
        } catch (BusinessException e) {
            log.error("Failed to remove catalogue link for invertebrate {}", invertebrate.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
