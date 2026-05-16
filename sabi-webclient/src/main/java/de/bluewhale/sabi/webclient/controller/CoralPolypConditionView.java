/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralPolypConditionTo;
import de.bluewhale.sabi.model.PolypCondition;
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

/**
 * JSF CDI-Bean controller for coral polyp condition history (US4).
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralPolypConditionView implements Serializable {

    @Autowired
    CoralStockService coralStockService;

    @Inject
    UserSession userSession;

    @Inject
    CoralEntryNavContext coralEntryNavContext;

    private List<CoralPolypConditionTo> polypHistory = new ArrayList<>();

    /** New observation form fields */
    private LocalDate newObservedOn = LocalDate.now();
    private PolypCondition newCondition = PolypCondition.VITAL;

    /** Record being edited */
    private CoralPolypConditionTo editingRecord;

    @PostConstruct
    public void init() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null) return;
        try {
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load polyp history for coral {}", coralId, e);
        }
    }

    public PolypCondition[] getPolypConditions() {
        return PolypCondition.values();
    }

    public void onAddObservation() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null || newCondition == null) return;
        CoralPolypConditionTo obs = new CoralPolypConditionTo();
        obs.setCoralStockEntryId(coralId);
        obs.setObservedOn(newObservedOn != null ? newObservedOn : LocalDate.now());
        obs.setCondition(newCondition);
        try {
            coralStockService.addPolypObservation(coralId, obs, userSession.getSabiBackendToken());
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
            newObservedOn = LocalDate.now();
            newCondition = PolypCondition.VITAL;
        } catch (BusinessException e) {
            log.error("Failed to add polyp observation for coral {}", coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeleteObservation(CoralPolypConditionTo obs) {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null) return;
        try {
            coralStockService.deletePolypObservation(coralId, obs.getId(), userSession.getSabiBackendToken());
            polypHistory.remove(obs);
        } catch (BusinessException e) {
            log.error("Failed to delete polyp observation {} for coral {}", obs.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onStartEditObservation(CoralPolypConditionTo obs) {
        this.editingRecord = obs;
    }

    public void onSaveEditObservation() {
        Long coralId = coralEntryNavContext.getEntry() != null
                ? coralEntryNavContext.getEntry().getId() : null;
        if (coralId == null || editingRecord == null) return;
        try {
            coralStockService.updatePolypObservation(coralId, editingRecord, userSession.getSabiBackendToken());
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
            editingRecord = null;
        } catch (BusinessException e) {
            log.error("Failed to update polyp observation {} for coral {}", editingRecord.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}

