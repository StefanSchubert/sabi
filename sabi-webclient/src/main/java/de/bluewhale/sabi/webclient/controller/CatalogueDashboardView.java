/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralCatalogueAdminService;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueAdminService;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateCatalogueAdminService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;

/**
 * JSF CDI-Bean controller for the catalogue admin dashboard.
 * Provides KPI counts (pending / approved) for fish and coral catalogues.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Slf4j
public class CatalogueDashboardView implements Serializable {

    @Autowired
    FishCatalogueAdminService fishCatalogueAdminService;

    @Autowired
    CoralCatalogueAdminService coralCatalogueAdminService;

    @Autowired
    InvertebrateCatalogueAdminService invertebrateCatalogueAdminService;

    @Inject
    UserSession userSession;

    private int fishPendingCount  = 0;
    private int fishApprovedCount = 0;
    private int coralPendingCount  = 0;
    private int coralApprovedCount = 0;
    private int invertebratePendingCount  = 0;
    private int invertebrateApprovedCount = 0;

    @PostConstruct
    public void init() {
        String token = userSession.getSabiBackendToken();

        try {
            var fishAll     = fishCatalogueAdminService.getAllEntries(token);
            var fishPending = fishCatalogueAdminService.getPendingProposals(token);
            fishPendingCount  = fishPending != null ? fishPending.size() : 0;
            fishApprovedCount = fishAll != null
                    ? (int) fishAll.stream()
                        .filter(e -> "PUBLIC".equals(e.getStatus() != null ? e.getStatus().name() : ""))
                        .count()
                    : 0;
        } catch (BusinessException e) {
            log.warn("Failed to load fish catalogue stats for dashboard", e);
        }

        try {
            var coralAll     = coralCatalogueAdminService.listAll(token);
            var coralPending = coralCatalogueAdminService.listPending(token);
            coralPendingCount  = coralPending != null ? coralPending.size() : 0;
            coralApprovedCount = coralAll != null
                    ? (int) coralAll.stream()
                        .filter(e -> e.getStatus() != null && "PUBLIC".equals(e.getStatus().name()))
                        .count()
                    : 0;
        } catch (BusinessException e) {
            log.warn("Failed to load coral catalogue stats for dashboard", e);
        }

        try {
            var invertAll     = invertebrateCatalogueAdminService.listAll(token);
            var invertPending = invertebrateCatalogueAdminService.listPending(token);
            invertebratePendingCount  = invertPending != null ? invertPending.size() : 0;
            invertebrateApprovedCount = invertAll != null
                    ? (int) invertAll.stream()
                        .filter(e -> e.getStatus() != null && "PUBLIC".equals(e.getStatus().name()))
                        .count()
                    : 0;
        } catch (BusinessException e) {
            log.warn("Failed to load invertebrate catalogue stats for dashboard", e);
        }
    }
}


