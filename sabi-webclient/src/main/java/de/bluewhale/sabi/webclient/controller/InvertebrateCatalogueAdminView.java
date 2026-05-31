/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateCatalogueAdminService;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateCatalogueService;
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
 * JSF CDI-Bean controller for admin invertebrate catalogue management.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Getter
@Setter
@Slf4j
public class InvertebrateCatalogueAdminView implements Serializable {

    private static final List<String> SUPPORTED_LANGS = Arrays.asList("de", "en", "es", "fr", "it");

    @Autowired
    InvertebrateCatalogueAdminService invertebrateCatalogueAdminService;

    @Autowired
    InvertebrateCatalogueService invertebrateCatalogueService;

    @Inject
    UserSession userSession;

    private List<InvertebrateCatalogueEntryTo> pendingProposals = new ArrayList<>();
    private List<InvertebrateCatalogueEntryTo> catalogueEntries = new ArrayList<>();
    private InvertebrateCatalogueEntryTo selectedEntry;
    private InvertebrateCatalogueEntryTo selectedProposal;
    private String rejectionReason;

    @PostConstruct
    public void init() {
        refreshProposals();
        refreshCatalogue();
    }

    public void refreshProposals() {
        try {
            pendingProposals = invertebrateCatalogueAdminService.listPending(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load pending invertebrate catalogue proposals", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void refreshCatalogue() {
        try {
            catalogueEntries = invertebrateCatalogueAdminService.listAll(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to load invertebrate catalogue entries", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onSelectEntry(InvertebrateCatalogueEntryTo entry) {
        if (entry == null) return;
        List<InvertebrateCatalogueI18nTo> i18n = entry.getI18nEntries();
        if (i18n == null) {
            i18n = new ArrayList<>();
            entry.setI18nEntries(i18n);
        }
        for (String lang : SUPPORTED_LANGS) {
            final String langCode = lang;
            boolean exists = i18n.stream().anyMatch(e -> langCode.equals(e.getLanguageCode()));
            if (!exists) {
                InvertebrateCatalogueI18nTo slot = new InvertebrateCatalogueI18nTo();
                slot.setLanguageCode(langCode);
                i18n.add(slot);
            }
        }
        i18n.sort((a, b) -> {
            int ia = SUPPORTED_LANGS.indexOf(a.getLanguageCode());
            int ib = SUPPORTED_LANGS.indexOf(b.getLanguageCode());
            return Integer.compare(ia < 0 ? 99 : ia, ib < 0 ? 99 : ib);
        });
        this.selectedEntry = entry;
    }

    public void onApproveEntry() {
        if (selectedEntry == null) return;
        try {
            invertebrateCatalogueAdminService.approve(selectedEntry.getId(), userSession.getSabiBackendToken());
            MessageUtil.info(null, "invertebratecatalogue.admin.approve.success", userSession.getLocale());
            Long idToReSelect = selectedEntry.getId();
            refreshProposals();
            refreshCatalogue();
            catalogueEntries.stream()
                    .filter(e -> idToReSelect.equals(e.getId()))
                    .findFirst()
                    .ifPresent(this::onSelectEntry);
        } catch (BusinessException e) {
            log.error("Failed to approve invertebrate catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onRejectEntry() {
        if (selectedEntry == null) return;
        try {
            invertebrateCatalogueAdminService.reject(selectedEntry.getId(), userSession.getSabiBackendToken());
            MessageUtil.info(null, "invertebratecatalogue.admin.reject.success", userSession.getLocale());
            Long idToReSelect = selectedEntry.getId();
            refreshProposals();
            refreshCatalogue();
            catalogueEntries.stream()
                    .filter(e -> idToReSelect.equals(e.getId()))
                    .findFirst()
                    .ifPresent(this::onSelectEntry);
        } catch (BusinessException e) {
            log.error("Failed to reject invertebrate catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onSaveEntry() {
        if (selectedEntry == null) return;
        try {
            Long idToReSelect = selectedEntry.getId();
            invertebrateCatalogueAdminService.adminUpdate(selectedEntry.getId(), selectedEntry,
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
            log.error("Failed to save invertebrate catalogue entry {}", selectedEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onCancelEdit() {
        this.selectedEntry = null;
    }

    public void onOpenProposal(InvertebrateCatalogueEntryTo proposal) {
        this.selectedProposal = proposal;
        this.rejectionReason = null;
    }

    public void onApprove() {
        if (selectedProposal == null) return;
        try {
            invertebrateCatalogueAdminService.approve(selectedProposal.getId(), userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "invertebratecatalogue.admin.approve.success", userSession.getLocale());
            refreshProposals();
            refreshCatalogue();
        } catch (BusinessException e) {
            log.error("Failed to approve invertebrate catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onReject() {
        if (selectedProposal == null) return;
        try {
            invertebrateCatalogueAdminService.reject(selectedProposal.getId(), userSession.getSabiBackendToken());
            selectedProposal = null;
            MessageUtil.info(null, "invertebratecatalogue.admin.reject.success", userSession.getLocale());
            refreshProposals();
            refreshCatalogue();
        } catch (BusinessException e) {
            log.error("Failed to reject invertebrate catalogue entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public InvertebrateTaxonomicCategory[] getTaxonomicCategories() {
        return InvertebrateTaxonomicCategory.values();
    }

    public CoralCareLevel[] getCareLevels() {
        return CoralCareLevel.values();
    }
}
