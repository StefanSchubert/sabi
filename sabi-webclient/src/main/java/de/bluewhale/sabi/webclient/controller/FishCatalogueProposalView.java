/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * Part of 002-fish-stock-catalogue - T056 + T070 (edit mode)
 */
package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueI18nTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueService;
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
import java.text.MessageFormat;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSF CDI-Bean for proposing or editing a fish catalogue entry.
 * Supports both "new proposal" (P4) and "edit existing entry" (P6) modes.
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class FishCatalogueProposalView implements Serializable {

    private static final List<String> LANGUAGE_CODES = List.of("de", "en", "es", "fr", "it");

    @Autowired
    FishCatalogueService fishCatalogueService;

    @Inject
    UserSession userSession;

    private FishCatalogueEntryTo proposal;
    private boolean editMode = false;
    private boolean duplicateWarningShown = false;
    private String duplicateWarningMessage = "";
    private boolean submittedSuccessfully = false;
    private List<FishCatalogueSearchResultTo> catalogueEntries = new ArrayList<>();

    @PostConstruct
    public void init() {
        proposal = new FishCatalogueEntryTo();
        proposal.setI18nEntries(buildEmptyI18nList());
    }

    /** Called when editing an existing catalogue entry (T070). */
    public void initEdit(FishCatalogueEntryTo existing) {
        this.proposal = existing;
        this.editMode = true;
        // Ensure all 5 language slots are present
        if (proposal.getI18nEntries() == null) {
            proposal.setI18nEntries(buildEmptyI18nList());
        } else {
            // Fill missing language slots
            for (int i = 0; i < LANGUAGE_CODES.size(); i++) {
                if (i >= proposal.getI18nEntries().size()) {
                    FishCatalogueI18nTo slot = new FishCatalogueI18nTo();
                    slot.setLanguageCode(LANGUAGE_CODES.get(i));
                    proposal.getI18nEntries().add(slot);
                }
            }
        }
    }

    /** Triggered on blur of scientific name field — checks for duplicates (FR-015). */
    public void onScientificNameBlur() {
        duplicateWarningShown = false;
        duplicateWarningMessage = "";
        String name = proposal.getScientificName();
        if (name == null || name.isBlank()) return;
        try {
            boolean duplicate = fishCatalogueService.isDuplicate(name, userSession.getSabiBackendToken());
            if (duplicate) {
                duplicateWarningShown = true;
                // Use MessageFormat to insert the scientific name into the i18n message
                String template = MessageUtil.getFromMessageProperties(
                        "fishcatalogue.scientificname.duplicate.warning", userSession.getLocale());
                duplicateWarningMessage = template != null
                        ? MessageFormat.format(template, name)
                        : "Duplicate: " + name;
            }
        } catch (BusinessException e) {
            log.warn("Duplicate check failed: {}", e.getMessage());
        }
    }

    /** Submit the proposal (new or edit). */
    public void onSubmit() {
        String token = userSession.getSabiBackendToken();
        log.info("onSubmit: token prefix='{}', editMode={}",
                token != null && token.length() > 10 ? token.substring(0, 10) + "..." : token,
                editMode);
        if (token == null || "N/A".equals(token) || !token.startsWith("Bearer ")) {
            log.error("onSubmit: invalid backend token='{}' — user needs to re-login", token);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return;
        }
        try {
            if (editMode) {
                fishCatalogueService.updateEntry(proposal, userSession.getSabiBackendToken());
                MessageUtil.info(null, "fishcatalogue.admin.approve.button", userSession.getLocale());
            } else {
                fishCatalogueService.propose(proposal, userSession.getSabiBackendToken());
                MessageUtil.info(null, "fishcatalogue.propose.title", userSession.getLocale());
                // Load full catalogue to show status overview
                try {
                    catalogueEntries = fishCatalogueService.listAll(
                            userSession.getLanguage(), userSession.getSabiBackendToken());
                } catch (BusinessException ex) {
                    log.warn("Could not load catalogue list after proposal: {}", ex.getMessage());
                    catalogueEntries = Collections.emptyList();
                }
                submittedSuccessfully = true;
                // Reset form for potential next entry
                init();
            }
        } catch (BusinessException e) {
            log.error("Failed to submit catalogue proposal", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public String onCancel() {
        return "/secured/fishStockView?faces-redirect=true";
    }

    /** Reset to form mode for entering another proposal. */
    public void onNewProposal() {
        submittedSuccessfully = false;
        catalogueEntries = new ArrayList<>();
        init();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<FishCatalogueI18nTo> buildEmptyI18nList() {
        List<FishCatalogueI18nTo> list = new ArrayList<>();
        for (String lang : LANGUAGE_CODES) {
            FishCatalogueI18nTo entry = new FishCatalogueI18nTo();
            entry.setLanguageCode(lang);
            list.add(entry);
        }
        return list;
    }
}
