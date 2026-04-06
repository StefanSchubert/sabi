/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishStockService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSF CDI-Bean controller for the fish stock tab.
 * Manages the fish list split into active (no exodusOn) and departed entries.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class FishStockView implements Serializable {

    @Autowired
    FishStockService fishStockService;

    @Inject
    UserSession userSession;

    @Inject
    TankListView tankListView;

    /** Active fish (no exodusOn). */
    private List<FishStockEntryTo> activeFish = new ArrayList<>();

    /** Departed fish (exodusOn set). */
    private List<FishStockEntryTo> departedFish = new ArrayList<>();

    /** Currently selected fish for editing/departure. */
    private FishStockEntryTo selectedFish;

    /** Whether the departed-section is expanded (FR-007: default collapsed). */
    private boolean departedSectionExpanded = false;

    /**
     * Selected aquarium ID from the tank-selector dropdown on fishStockView.xhtml (T037).
     * Needed because fishStockView.xhtml has an inline tank selector.
     */
    private Long selectedAquariumId;

    @PostConstruct
    public void init() {
        log.debug("FishStockView init for user={}, aquarium={}",
                userSession.getUserName(),
                userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : "none");

        // Auto-preselect if the user has exactly one aquarium
        if (selectedAquariumId == null
                && tankListView.getTanks() != null
                && tankListView.getTanks().size() == 1) {
            selectedAquariumId = tankListView.getTanks().get(0).getId();
            log.debug("Auto-preselected single aquarium id={}", selectedAquariumId);
        }

        // Use selectedAquariumId from dropdown if set, otherwise fall back to session tank
        Long tankId = selectedAquariumId != null ? selectedAquariumId :
                (userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
        if (tankId == null) {
            return;
        }
        loadFishForTank(tankId);
    }

    /**
     * Called by p:ajax when user selects a tank from the dropdown on fishStockView.xhtml (T037).
     */
    public void onTankSelected() {
        if (selectedAquariumId == null) {
            activeFish.clear();
            departedFish.clear();
            return;
        }
        loadFishForTank(selectedAquariumId);
    }

    private void loadFishForTank(Long tankId) {
        try {
            List<FishStockEntryTo> all = fishStockService.getFishForTank(
                    tankId,
                    userSession.getSabiBackendToken());
            activeFish = all.stream()
                    .filter(f -> f.getExodusOn() == null)
                    .collect(Collectors.toList());
            departedFish = all.stream()
                    .filter(f -> f.getExodusOn() != null)
                    .collect(Collectors.toList());
        } catch (BusinessException e) {
            log.error("Failed to load fish for tank {}", tankId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l");
        }
    }

    public void onAddFish() {
        selectedFish = new FishStockEntryTo();
        selectedFish.setAquariumId(userSession.getSelectedTank() != null
                ? userSession.getSelectedTank().getId() : null);
    }

    public void onEditFish(FishStockEntryTo fish) {
        this.selectedFish = fish;
    }

    public void onDeleteFish(FishStockEntryTo fish) {
        try {
            fishStockService.deleteFish(fish.getId(), userSession.getSabiBackendToken());
            activeFish.remove(fish);
            departedFish.remove(fish);
        } catch (BusinessException e) {
            log.warn("Could not delete fish {}: {}", fish.getId(), e.getMessage());
            MessageUtil.error(null, "fishstock.delete.denied.label");
        }
    }

    public void onRecordDeparture(FishStockEntryTo fish) {
        this.selectedFish = fish;
    }

    public void onRemoveCatalogueLink(FishStockEntryTo fish) {
        try {
            fishStockService.removeCatalogueLink(fish.getId(), userSession.getSabiBackendToken());
            fish.setFishCatalogueId(null);
            fish.setScientificName(null);
        } catch (BusinessException e) {
            log.error("Failed to remove catalogue link for fish {}", fish.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l");
        }
    }
}




