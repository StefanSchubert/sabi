/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * Part of 002-fish-stock-catalogue - T064
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueI18nTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueAdminService;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueService;
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
 * JSF CDI-Bean for the admin catalogue management view (FR-016, FR-017, FR-021).
 */
@Named
@SessionScope
@Getter
@Setter
@Slf4j
public class FishCatalogueAdminView implements Serializable {

    private static final List<String> SUPPORTED_LANGS = Arrays.asList("de", "en", "es", "fr", "it");

    @Autowired
    FishCatalogueAdminService fishCatalogueAdminService;

    @Autowired
    FishCatalogueService fishCatalogueService;

    @Inject
    UserSession userSession;

    /** Pending proposals (proposals not yet approved/rejected). */
    private List<FishCatalogueEntryTo> pendingProposals = new ArrayList<>();

    /** Full catalogue — all entries regardless of status (admin view). */
    private List<FishCatalogueEntryTo> catalogueEntries = new ArrayList<>();

    /** Currently selected catalogue entry for inline editing. */
    private FishCatalogueEntryTo selectedEntry;

    /** Rejection reason for the pending-proposal dialog. */
    private String rejectionReason;

    /** Currently selected proposal in the pending-proposals dialog. */
    private FishCatalogueEntryTo selectedProposal;

    @PostConstruct
    public void init() {
        refreshProposals();
        refreshCatalogue();
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

    /** Reload the full catalogue list from backend (all statuses). */
    public void refreshCatalogue() {
        try {
            catalogueEntries = fishCatalogueAdminService.getAllEntries(
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load catalogue entries", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /**
     * Select a catalogue entry for inline editing.
     * Ensures all 5 language slots exist so EL indexing is safe.
     */
    public void onSelectEntry(FishCatalogueEntryTo entry) {
        if (entry == null) return;
        // Ensure i18n list has one slot per supported language
        List<FishCatalogueI18nTo> i18n = entry.getI18nEntries();
        if (i18n == null) {
            i18n = new ArrayList<>();
            entry.setI18nEntries(i18n);
        }
        for (String lang : SUPPORTED_LANGS) {
            final String langCode = lang;
            boolean exists = i18n.stream()
                    .anyMatch(e -> langCode.equals(e.getLanguageCode()));
            if (!exists) {
                FishCatalogueI18nTo slot = new FishCatalogueI18nTo();
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

    /** Save changes to the currently selected catalogue entry. */
    public void onSaveEntry() {
        if (selectedEntry == null) return;
        try {
            Long idToReSelect = selectedEntry.getId();
            fishCatalogueService.updateEntry(selectedEntry, userSession.getSabiBackendToken());
            MessageUtil.info(null, "common.save.confirmation.t", userSession.getLocale());
            refreshCatalogue();
            // Re-select the updated entry to reflect changes
            if (idToReSelect != null) {
                catalogueEntries.stream()
                        .filter(e -> idToReSelect.equals(e.getId()))
                        .findFirst()
                        .ifPresent(this::onSelectEntry);
            }
        } catch (BusinessException e) {
            log.error("Failed to save catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    /** Discard inline editor selection. */
    public void onCancelEdit() {
        this.selectedEntry = null;
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
            refreshCatalogue();
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
            refreshCatalogue();
        } catch (BusinessException e) {
            log.error("Failed to reject catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
