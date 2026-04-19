/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import org.primefaces.event.SelectEvent;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueService;
import de.bluewhale.sabi.webclient.apigateway.FishStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.file.UploadedFile;
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
    private byte[] previewPhoto;
    private UploadedFile uploadedFile;

    /**
     * Derived from currentEntry.id — survives RequestScope via hidden field.
     * Used in EL as {@code #{fishStockEntryView.edit}}.
     */
    public boolean isEdit() {
        return currentEntry != null && currentEntry.getId() != null;
    }

    public void init(FishStockEntryTo existing) {
        if (existing != null && existing.getId() != null) {
            this.currentEntry = existing;
        } else {
            this.currentEntry = new FishStockEntryTo();
            this.currentEntry.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            this.currentEntry.setAddedOn(LocalDate.now());
        }
    }

    public void onSave() {
        // Client-side validation: addedOn must not be in the future (FR-003)
        if (currentEntry.getAddedOn() != null && currentEntry.getAddedOn().isAfter(LocalDate.now())) {
            MessageUtil.error(null, "fishstock.form.entrydate.future.error", userSession.getLocale());
            return;
        }
        // Fallback: if aquariumId was lost across RequestScope boundary, recover from session
        if (currentEntry.getAquariumId() == null
                && userSession.getSelectedTank() != null
                && userSession.getSelectedTank().getId() != null) {
            currentEntry.setAquariumId(userSession.getSelectedTank().getId());
        }
        // Derive edit-mode from ID (survives RequestScope via hidden field, same pattern as measureView)
        boolean effectiveEdit = currentEntry.getId() != null;
        try {
            if (effectiveEdit) {
                fishStockService.updateFish(currentEntry, userSession.getSabiBackendToken());
            } else {
                ResultTo<FishStockEntryTo> result = fishStockService.addFish(currentEntry, userSession.getSabiBackendToken());
                // Capture the server-generated ID so we can upload the photo
                if (result != null && result.getValue() != null && result.getValue().getId() != null) {
                    currentEntry.setId(result.getValue().getId());
                }
            }
        } catch (BusinessException e) {
            log.error("Failed to save fish entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return;
        }

        // Upload photo if a file was selected (mode="simple" submits it with the save request)
        processPhotoUpload();
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
    public void onSelectCatalogue(SelectEvent<FishCatalogueSearchResultTo> event) {
        FishCatalogueSearchResultTo result = event.getObject();
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

    /**
     * Processes the uploaded file (if any) after the fish entry has been saved.
     * Called from onSave() — the p:fileUpload mode="simple" submits the file
     * together with the form data in the same request.
     */
    private void processPhotoUpload() {
        if (uploadedFile == null || uploadedFile.getContent() == null || uploadedFile.getSize() == 0) {
            return;
        }
        if (uploadedFile.getSize() > 5_242_880) {
            MessageUtil.error(null, "fishstock.form.photo.size.error", userSession.getLocale());
            return;
        }
        if (currentEntry.getId() == null) {
            log.warn("Cannot upload photo — fish entry has no ID after save");
            return;
        }
        try {
            byte[] fileBytes = uploadedFile.getContent();
            String contentType = uploadedFile.getContentType() != null
                    ? uploadedFile.getContentType() : "image/jpeg";
            fishStockService.uploadPhoto(currentEntry.getId(), fileBytes, contentType,
                    userSession.getSabiBackendToken());
            currentEntry.setHasPhoto(true);
            this.previewPhoto = fileBytes;
            log.info("Photo uploaded for fish {} ({} bytes, {})", currentEntry.getId(),
                    fileBytes.length, contentType);
        } catch (BusinessException e) {
            log.error("Failed to upload photo for fish {}", currentEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
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

