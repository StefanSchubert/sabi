/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * Part of 002-fish-stock-catalogue - T064
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueAdminService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JSF CDI-Bean for the admin catalogue management view (FR-016, FR-017, FR-021).
 */
@Named
@SessionScope
@Getter
@Setter
@Slf4j
public class FishCatalogueAdminView implements Serializable {

    @Autowired
    FishCatalogueAdminService fishCatalogueAdminService;

    @Inject
    UserSession userSession;

    private List<FishCatalogueEntryTo> pendingProposals = new ArrayList<>();
    private FishCatalogueEntryTo selectedProposal;
    private String rejectionReason;

    @PostConstruct
    public void init() {
        refreshProposals();
    }

    /** Reload pending proposals from backend. */
    public void refreshProposals() {
        try {
            pendingProposals = fishCatalogueAdminService.getPendingProposals(
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load pending proposals", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onOpenProposal(FishCatalogueEntryTo proposal) {
        this.selectedProposal = proposal;
        this.rejectionReason = null;
    }

    /** Approve the selected proposal (FR-016). */
    public void onApprove() {
        if (selectedProposal == null) return;
        try {
            fishCatalogueAdminService.approveEntry(
                    selectedProposal.getId(),
                    selectedProposal,
                    userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "fishcatalogue.status.public", userSession.getLocale());
            refreshProposals();
        } catch (BusinessException e) {
            log.error("Failed to approve catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Reject the selected proposal (FR-017). */
    public void onReject() {
        if (selectedProposal == null) return;
        try {
            fishCatalogueAdminService.rejectEntry(
                    selectedProposal.getId(),
                    rejectionReason,
                    userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "fishcatalogue.status.rejected", userSession.getLocale());
            refreshProposals();
        } catch (BusinessException e) {
            log.error("Failed to reject catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
