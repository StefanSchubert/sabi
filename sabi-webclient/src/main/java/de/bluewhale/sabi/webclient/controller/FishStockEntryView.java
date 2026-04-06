/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueService;
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
import java.util.Collections;
import java.util.List;

/**
 * JSF CDI-Bean controller for the fish stock entry form (add/edit).
 * Handles catalogue search and photo upload.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class FishStockEntryView implements Serializable {

    @Autowired
    FishStockService fishStockService;

    @Autowired
    FishCatalogueService fishCatalogueService;

    @Inject
    UserSession userSession;

    private FishStockEntryTo currentEntry = new FishStockEntryTo();
    private boolean isEdit = false;
    private byte[] previewPhoto;

    public void init(FishStockEntryTo existing) {
        if (existing != null && existing.getId() != null) {
            this.currentEntry = existing;
            this.isEdit = true;
        } else {
            this.currentEntry = new FishStockEntryTo();
            this.currentEntry.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            this.currentEntry.setAddedOn(LocalDate.now());
            this.isEdit = false;
        }
    }

    public void onSave() {
        // Client-side validation: addedOn must not be in the future (FR-003)
        if (currentEntry.getAddedOn() != null && currentEntry.getAddedOn().isAfter(LocalDate.now())) {
            MessageUtil.error(null, "fishstock.form.entrydate.future.error");
            return;
        }
        try {
            if (isEdit) {
                fishStockService.updateFish(currentEntry, userSession.getSabiBackendToken());
            } else {
                fishStockService.addFish(currentEntry, userSession.getSabiBackendToken());
            }
        } catch (BusinessException e) {
            log.error("Failed to save fish entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l");
        }
    }

    /** Catalogue search for autocomplete (min 2 chars). */
    public List<FishCatalogueSearchResultTo> onSearchCatalogue(String query) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        try {
            return fishCatalogueService.search(query,
                    userSession.getLocale().getLanguage(),
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Catalogue search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Called when user selects a catalogue entry from the autocomplete dropdown. */
    public void onSelectCatalogue(FishCatalogueSearchResultTo result) {
        if (result != null) {
            currentEntry.setScientificName(result.getScientificName());
            currentEntry.setExternalRefUrl(result.getReferenceUrl());
            currentEntry.setFishCatalogueId(result.getId());
        }
    }

    /** Remove the catalogue link from the current entry. */
    public void onRemoveCatalogueLink() {
        if (currentEntry.getId() != null) {
            try {
                fishStockService.removeCatalogueLink(currentEntry.getId(), userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                log.warn("Failed to remove catalogue link: {}", e.getMessage());
            }
        }
        currentEntry.setFishCatalogueId(null);
        currentEntry.setScientificName(null);
    }

    /** Photo upload handler. */
    public void onPhotoUpload(byte[] fileBytes, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) return;
        if (fileBytes.length > 5_242_880) {
            MessageUtil.error(null, "fishstock.form.photo.size.error");
            return;
        }
        if (currentEntry.getId() != null) {
            try {
                fishStockService.uploadPhoto(currentEntry.getId(), fileBytes, contentType,
                        userSession.getSabiBackendToken());
                currentEntry.setHasPhoto(true);
                this.previewPhoto = fileBytes;
            } catch (BusinessException e) {
                log.error("Failed to upload photo", e);
                MessageUtil.error(null, "common.error.backend_unreachable.l");
            }
        }
    }

    public void onDeletePhoto() {
        if (currentEntry.getId() != null) {
            // Deferred to after-save scenario; for new entries, just clear preview
            previewPhoto = null;
            currentEntry.setHasPhoto(false);
        }
    }
}

