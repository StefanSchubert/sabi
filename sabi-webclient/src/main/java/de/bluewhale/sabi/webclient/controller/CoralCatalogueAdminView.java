/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.CoralCatalogueEntryTo;
import de.bluewhale.sabi.model.CoralCatalogueI18nTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralCatalogueAdminService;
import de.bluewhale.sabi.webclient.apigateway.CoralCatalogueService;
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
import java.util.Arrays;
import java.util.List;

/**
 * JSF CDI-Bean controller for admin coral catalogue management (US7).
 * Part of 005-coral-stock.
 */
@Named
@SessionScope
@Getter
@Setter
@Slf4j
public class CoralCatalogueAdminView implements Serializable {

    private static final List<String> SUPPORTED_LANGS = Arrays.asList("de", "en", "es", "fr", "it");

    @Autowired
    CoralCatalogueAdminService coralCatalogueAdminService;

    /** Used for saving existing PUBLIC entries (admin edit). */
    @Autowired
    CoralCatalogueService coralCatalogueService;

    @Inject
    UserSession userSession;

    private List<CoralCatalogueEntryTo> pendingProposals = new ArrayList<>();
    private List<CoralCatalogueEntryTo> catalogueEntries = new ArrayList<>();
    private CoralCatalogueEntryTo selectedEntry;
    private CoralCatalogueEntryTo selectedProposal;
    private String rejectionReason;

    @PostConstruct
    public void init() {
        refreshProposals();
        refreshCatalogue();
    }

    /** Reload pending proposals from backend. */
    public void refreshProposals() {
        try {
            pendingProposals = coralCatalogueAdminService.listPending(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load pending coral catalogue proposals", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Reload ALL catalogue entries for the catalogue browser. */
    public void refreshCatalogue() {
        try {
            catalogueEntries = coralCatalogueAdminService.listAll(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load coral catalogue entries", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /**
     * Select a catalogue entry for inline editing.
     * Ensures all 5 language slots exist so EL indexing is safe.
     */
    public void onSelectEntry(CoralCatalogueEntryTo entry) {
        if (entry == null) return;
        List<CoralCatalogueI18nTo> i18n = entry.getI18nEntries();
        if (i18n == null) {
            i18n = new ArrayList<>();
            entry.setI18nEntries(i18n);
        }
        for (String lang : SUPPORTED_LANGS) {
            final String langCode = lang;
            boolean exists = i18n.stream().anyMatch(e -> langCode.equals(e.getLanguageCode()));
            if (!exists) {
                CoralCatalogueI18nTo slot = new CoralCatalogueI18nTo();
                slot.setLanguageCode(langCode);
                i18n.add(slot);
            }
        }
        // Sort to fixed order: de, en, es, fr, it
        i18n.sort((a, b) -> {
            int ia = SUPPORTED_LANGS.indexOf(a.getLanguageCode());
            int ib = SUPPORTED_LANGS.indexOf(b.getLanguageCode());
            return Integer.compare(ia < 0 ? 99 : ia, ib < 0 ? 99 : ib);
        });
        this.selectedEntry = entry;
    }

    /** Approve the currently selected entry in the inline editor (PENDING → PUBLIC). */
    public void onApproveEntry() {
        if (selectedEntry == null) return;
        try {
            coralCatalogueAdminService.approve(selectedEntry.getId(), userSession.getSabiBackendToken());
            MessageUtil.info(null, "coralcatalogue.admin.approve.success", userSession.getLocale());
            Long idToReSelect = selectedEntry.getId();
            refreshProposals();
            refreshCatalogue();
            // Re-select so editor stays open showing the new PUBLIC status
            catalogueEntries.stream()
                    .filter(e -> idToReSelect.equals(e.getId()))
                    .findFirst()
                    .ifPresent(this::onSelectEntry);
        } catch (BusinessException e) {
            log.error("Failed to approve coral catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Reject the currently selected entry in the inline editor (PENDING → REJECTED). */
    public void onRejectEntry() {
        if (selectedEntry == null) return;
        try {
            coralCatalogueAdminService.reject(selectedEntry.getId(), userSession.getSabiBackendToken());
            MessageUtil.info(null, "coralcatalogue.admin.reject.success", userSession.getLocale());
            Long idToReSelect = selectedEntry.getId();
            refreshProposals();
            refreshCatalogue();
            catalogueEntries.stream()
                    .filter(e -> idToReSelect.equals(e.getId()))
                    .findFirst()
                    .ifPresent(this::onSelectEntry);
        } catch (BusinessException e) {
            log.error("Failed to reject coral catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Save changes to the currently selected catalogue entry. */
    public void onSaveEntry() {
        if (selectedEntry == null) return;
        try {
            Long idToReSelect = selectedEntry.getId();
            coralCatalogueAdminService.adminUpdate(selectedEntry.getId(), selectedEntry,
                    userSession.getSabiBackendToken());
            MessageUtil.info(null, "common.save.confirmation.t", userSession.getLocale());
            refreshCatalogue();
            if (idToReSelect != null) {
                catalogueEntries.stream()
                        .filter(e -> idToReSelect.equals(e.getId()))
                        .findFirst()
                        .ifPresent(this::onSelectEntry);
            }
        } catch (BusinessException e) {
            log.error("Failed to save coral catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Discard inline editor selection. */
    public void onCancelEdit() {
        this.selectedEntry = null;
    }

    /** Open a pending proposal in the detail dialog. */
    public void onOpenProposal(CoralCatalogueEntryTo proposal) {
        this.selectedProposal = proposal;
        this.rejectionReason = null;
    }

    /** Approve the selected proposal (sets status to PUBLIC). */
    public void onApprove() {
        if (selectedProposal == null) return;
        try {
            coralCatalogueAdminService.approve(selectedProposal.getId(), userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "coralcatalogue.admin.approve.success", userSession.getLocale());
            refreshProposals();
            refreshCatalogue();
        } catch (BusinessException e) {
            log.error("Failed to approve coral catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Reject the selected proposal. */
    public void onReject() {
        if (selectedProposal == null) return;
        try {
            coralCatalogueAdminService.reject(selectedProposal.getId(), userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "coralcatalogue.admin.reject.success", userSession.getLocale());
            refreshProposals();
            refreshCatalogue();
        } catch (BusinessException e) {
            log.error("Failed to reject coral catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
