/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralDepartureReason;
import de.bluewhale.sabi.model.InvertebrateDepartureRecordTo;
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

/**
 * JSF CDI-Bean controller for the invertebrate departure form.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class InvertebrateDepartureView implements Serializable {

    @Autowired
    InvertebrateStockService invertebrateStockService;

    @Inject
    UserSession userSession;

    @Inject
    InvertebrateEntryNavContext invertebrateEntryNavContext;

    private Long invertebrateId;
    private LocalDate invertebrateAddedOn;
    private String speciesName;
    private InvertebrateDepartureRecordTo departureRecord = new InvertebrateDepartureRecordTo();

    @PostConstruct
    public void init() {
        InvertebrateStockEntryTo ctx = invertebrateEntryNavContext.getEntry();
        if (ctx != null) {
            this.invertebrateId = ctx.getId();
            this.invertebrateAddedOn = ctx.getAddedOn();
            this.speciesName = ctx.getSpeciesName();
        }
        this.departureRecord = new InvertebrateDepartureRecordTo();
        this.departureRecord.setDepartedOn(LocalDate.now());
        this.departureRecord.setDepartureReason(CoralDepartureReason.DIED);
    }

    public CoralDepartureReason[] getDepartureReasons() {
        return CoralDepartureReason.values();
    }

    public String onSave() {
        if (invertebrateId == null) {
            log.error("onSave() called with null invertebrateId - cannot record departure");
            MessageUtil.error(null, "invertebratestock.delete.denied.label", userSession.getLocale());
            return null;
        }
        if (departureRecord.getDepartedOn() == null) {
            MessageUtil.error(null, "invertebratestock.departure.date.required", userSession.getLocale());
            return null;
        }
        if (invertebrateAddedOn != null && departureRecord.getDepartedOn().isBefore(invertebrateAddedOn)) {
            MessageUtil.error(null, "invertebratestock.departure.date.before_entry", userSession.getLocale());
            return null;
        }
        try {
            invertebrateStockService.recordDeparture(invertebrateId, departureRecord, userSession.getSabiBackendToken());
            invertebrateEntryNavContext.clear();
            return "/secured/invertebrateStockTab?faces-redirect=true";
        } catch (BusinessException e) {
            log.error("Failed to record departure for invertebrate {}", invertebrateId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
    }
}
