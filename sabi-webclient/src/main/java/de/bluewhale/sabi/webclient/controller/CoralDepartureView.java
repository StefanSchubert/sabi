/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralDepartureReason;
import de.bluewhale.sabi.model.CoralDepartureRecordTo;
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

/**
 * JSF CDI-Bean controller for the coral departure form (US2).
 * Part of 005-coral-stock.
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralDepartureView implements Serializable {

    @Autowired
    CoralStockService coralStockService;

    @Inject
    UserSession userSession;

    @Inject
    CoralEntryNavContext coralEntryNavContext;

    private Long coralId;
    private LocalDate coralAddedOn;
    private String speciesName;
    private CoralDepartureRecordTo departureRecord = new CoralDepartureRecordTo();

    @PostConstruct
    public void init() {
        CoralStockEntryTo ctx = coralEntryNavContext.getEntry();
        if (ctx != null) {
            this.coralId = ctx.getId();
            this.coralAddedOn = ctx.getAddedOn();
            this.speciesName = ctx.getSpeciesName();
        }
        this.departureRecord = new CoralDepartureRecordTo();
        this.departureRecord.setDepartureDate(LocalDate.now());
        this.departureRecord.setDepartureReason(CoralDepartureReason.DIED);
    }

    public CoralDepartureReason[] getDepartureReasons() {
        return CoralDepartureReason.values();
    }

    public String onSave() {
        if (coralId == null) {
            log.error("onSave() called with null coralId - cannot record departure");
            MessageUtil.error(null, "coralstock.delete.denied.label", userSession.getLocale());
            return null;
        }
        if (departureRecord.getDepartureDate() == null) {
            MessageUtil.error(null, "coralstock.departure.date.required", userSession.getLocale());
            return null;
        }
        if (coralAddedOn != null && departureRecord.getDepartureDate().isBefore(coralAddedOn)) {
            MessageUtil.error(null, "coralstock.departure.date.before_entry", userSession.getLocale());
            return null;
        }
        try {
            coralStockService.recordDeparture(coralId, departureRecord, userSession.getSabiBackendToken());
            coralEntryNavContext.clear();
            return "/secured/coralStockView?faces-redirect=true";
        } catch (BusinessException e) {
            log.error("Failed to record departure for coral {}", coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
    }
}

