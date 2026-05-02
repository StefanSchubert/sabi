/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.model.FishRoleTo;
import de.bluewhale.sabi.model.FishSizeHistoryTo;
import org.primefaces.event.SelectEvent;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishCatalogueService;
import de.bluewhale.sabi.webclient.apigateway.FishStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
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

    @Inject
    FishEntryNavContext fishEntryNavContext;

    private FishStockEntryTo currentEntry = new FishStockEntryTo();
    private byte[] previewPhoto;
    private UploadedFile uploadedFile;
    private List<FishRoleTo> availableRoles = Collections.emptyList();
    private List<FishSizeHistoryTo> sizeHistory = Collections.emptyList();

    /** Map from role ID → selected; populated in init(), used by per-role checkboxes. */
    private java.util.Map<Integer, Boolean> roleSelectionMap = new java.util.LinkedHashMap<>();

    /** New size entry: size in cm (null = user did not enter anything). */
    private java.math.BigDecimal newSizeCm;

    /** New size entry: date (defaults to today). */
    private java.time.LocalDate newSizeDate = java.time.LocalDate.now();

    /**
     * Derived from currentEntry.id — survives RequestScope via hidden field.
     * Used in EL as {@code #{fishStockEntryView.edit}}.
     */
    public boolean isEdit() {
        return currentEntry != null && currentEntry.getId() != null;
    }

    public void init(FishStockEntryTo existing) {
        if (existing != null && existing.getId() != null) {
            // Edit mode: use existing entry as-is
            this.currentEntry = existing;
        } else if (existing != null) {
            // Add or duplicate: keep all pre-filled data (commonName, scientificName, …)
            this.currentEntry = existing;
            if (this.currentEntry.getAquariumId() == null
                    && userSession.getSelectedTank() != null) {
                this.currentEntry.setAquariumId(userSession.getSelectedTank().getId());
            }
            if (this.currentEntry.getAddedOn() == null) {
                this.currentEntry.setAddedOn(LocalDate.now());
            }
        } else {
            // Null: fresh new entry
            this.currentEntry = new FishStockEntryTo();
            this.currentEntry.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            this.currentEntry.setAddedOn(LocalDate.now());
        }
        // Load available fish roles for the current user's language
        try {
            this.availableRoles = fishStockService.getFishRoles(
                    userSession.getLocale().getLanguage(),
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Could not load fish roles", e);
            this.availableRoles = Collections.emptyList();
        }
        // Build role selection map (true = already assigned to this fish)
        roleSelectionMap = new java.util.LinkedHashMap<>();
        for (FishRoleTo role : availableRoles) {
            roleSelectionMap.put(role.getId(),
                    currentEntry.getFishRoleIds() != null
                    && currentEntry.getFishRoleIds().contains(role.getId()));
        }
        // Load size history for edit mode
        if (currentEntry.getId() != null) {
            try {
                this.sizeHistory = fishStockService.getSizeHistory(
                        currentEntry.getId(), userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                log.warn("Could not load size history for fish {}", currentEntry.getId(), e);
                this.sizeHistory = Collections.emptyList();
            }
        }
    }

    /**
     * Initialises the form from the navigation context set by FishStockView before navigating here.
     * Called once per request (standalone page replaces former dialog).
     * In edit mode (existing ID), fresh data is always fetched from the backend to avoid stale
     * NavContext data (e.g. externalRefUrl missing if not yet refreshed in list view).
     */
    @PostConstruct
    public void postConstruct() {
        FishStockEntryTo ctxEntry = fishEntryNavContext.getEntry();
        if (ctxEntry != null && ctxEntry.getId() != null) {
            // Edit mode: reload fresh from backend to ensure all fields (especially externalRefUrl) are current
            try {
                FishStockEntryTo fresh = fishStockService.getFishById(
                        ctxEntry.getId(), userSession.getSabiBackendToken());
                if (fresh != null) {
                    init(fresh);
                } else {
                    // Fallback to NavContext data if backend returns nothing (e.g. 403)
                    init(ctxEntry);
                }
            } catch (BusinessException e) {
                log.warn("Could not reload fish {} from backend, using NavContext data", ctxEntry.getId(), e);
                init(ctxEntry);
            }
        } else if (ctxEntry != null) {
            init(ctxEntry);
        } else {
            // Fallback: page opened without navigation context
            FishStockEntryTo fallback = new FishStockEntryTo();
            fallback.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            fallback.setAddedOn(LocalDate.now());
            init(fallback);
        }
    }

    public void onSave() {
        // Client-side validation: addedOn must not be in the future (FR-003)
        if (currentEntry.getAddedOn() != null && currentEntry.getAddedOn().isAfter(LocalDate.now())) {
            MessageUtil.error(null, "fishstock.form.entrydate.future.error", userSession.getLocale());
            return;
        }
        // Rebuild fishRoleIds from roleSelectionMap
        java.util.List<Integer> selectedRoles = roleSelectionMap.entrySet().stream()
                .filter(java.util.Map.Entry::getValue)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        currentEntry.setFishRoleIds(selectedRoles);

        // Fallback: recover aquariumId from session
        if (currentEntry.getAquariumId() == null
                && userSession.getSelectedTank() != null
                && userSession.getSelectedTank().getId() != null) {
            currentEntry.setAquariumId(userSession.getSelectedTank().getId());
        }
        boolean effectiveEdit = currentEntry.getId() != null;
        try {
            if (effectiveEdit) {
                fishStockService.updateFish(currentEntry, userSession.getSabiBackendToken());
            } else {
                ResultTo<FishStockEntryTo> result = fishStockService.addFish(currentEntry, userSession.getSabiBackendToken());
                if (result != null && result.getValue() != null && result.getValue().getId() != null) {
                    currentEntry.setId(result.getValue().getId());
                }
            }
        } catch (BusinessException e) {
            log.error("Failed to save fish entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return;
        }

        // Save new size record if provided
        if (newSizeCm != null && newSizeCm.compareTo(java.math.BigDecimal.ZERO) > 0
                && currentEntry.getId() != null) {
            try {
                FishSizeHistoryTo sizeRecord = new FishSizeHistoryTo();
                sizeRecord.setFishStockEntryId(currentEntry.getId());
                sizeRecord.setMeasuredOn(newSizeDate != null ? newSizeDate : java.time.LocalDate.now());
                sizeRecord.setSizeCm(newSizeCm);
                fishStockService.addSizeRecord(currentEntry.getId(), sizeRecord, userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                log.warn("Could not save size record for fish {}", currentEntry.getId(), e);
            }
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
