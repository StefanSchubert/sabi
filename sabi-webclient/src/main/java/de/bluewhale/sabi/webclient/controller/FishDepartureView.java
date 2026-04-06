/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.DepartureReason;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
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
 * JSF CDI-Bean controller for the fish departure dialog (US2).
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class FishDepartureView implements Serializable {

    @Autowired
    FishStockService fishStockService;

    @Inject
    UserSession userSession;

    private Long fishId;
    private LocalDate fishAddedOn;
    private FishDepartureRecordTo departureRecord = new FishDepartureRecordTo();

    public void init(FishStockEntryTo fish) {
        if (fish != null) {
            this.fishId = fish.getId();
            this.fishAddedOn = fish.getAddedOn();
            this.departureRecord = new FishDepartureRecordTo();
            this.departureRecord.setDepartureDate(LocalDate.now());
            this.departureRecord.setDepartureReason(DepartureReason.UNKNOWN);
        }
    }

    public void onSave() {
        // Client-side validation: departureDate >= fishAddedOn (FR-006)
        if (departureRecord.getDepartureDate() == null) {
            MessageUtil.error(null, "fishstock.departure.date.required");
            return;
        }
        if (fishAddedOn != null && departureRecord.getDepartureDate().isBefore(fishAddedOn)) {
            MessageUtil.error(null, "fishstock.departure.date.after.error");
            return;
        }
        try {
            fishStockService.recordDeparture(fishId, departureRecord, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to record departure for fish {}", fishId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l");
        }
    }
}

