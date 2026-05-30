/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateCatalogueService;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateStockService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.RequestScope;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * JSF CDI-Bean controller for the invertebrate stock entry form (add/edit).
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class InvertebrateStockEntryView implements Serializable {

    @Autowired
    InvertebrateStockService invertebrateStockService;

    @Autowired
    InvertebrateCatalogueService invertebrateCatalogueService;

    @Inject
    UserSession userSession;

    @Inject
    InvertebrateEntryNavContext invertebrateEntryNavContext;

    private InvertebrateStockEntryTo currentEntry = new InvertebrateStockEntryTo();
    private byte[] previewPhoto;
    private UploadedFile uploadedFile;

    /** All available taxonomic categories for the dropdown. */
    public InvertebrateTaxonomicCategory[] getTaxonomicCategories() {
        return InvertebrateTaxonomicCategory.values();
    }

    /** All available care levels for the dropdown. */
    public CoralCareLevel[] getCareLevels() {
        return CoralCareLevel.values();
    }

    /** All available mobility options for the dropdown. */
    public InvertebrateMobility[] getMobilityOptions() {
        return InvertebrateMobility.values();
    }

    /** All available ecological roles for the dropdown. */
    public InvertebrateEcologicalRole[] getEcologicalRoles() {
        return InvertebrateEcologicalRole.values();
    }

    /** All available activity patterns for the dropdown. */
    public InvertebrateActivityPattern[] getActivityPatterns() {
        return InvertebrateActivityPattern.values();
    }

    /** Derived: true = update, false = add. */
    public boolean isEdit() { return currentEntry != null && currentEntry.getId() != null; }

    @PostConstruct
    public void postConstruct() {
        InvertebrateStockEntryTo ctxEntry = invertebrateEntryNavContext.getEntry();
        if (ctxEntry != null && ctxEntry.getId() != null) {
            // Edit mode: reload fresh from backend
            try {
                InvertebrateStockEntryTo fresh = invertebrateStockService.getInvertebrateById(
                        ctxEntry.getId(), userSession.getSabiBackendToken());
                init(fresh != null ? fresh : ctxEntry);
            } catch (BusinessException e) {
                log.warn("Could not reload invertebrate {} from backend, using NavContext data", ctxEntry.getId(), e);
                init(ctxEntry);
            }
        } else if (ctxEntry != null) {
            init(ctxEntry);
        } else {
            InvertebrateStockEntryTo fallback = new InvertebrateStockEntryTo();
            fallback.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            fallback.setAddedOn(LocalDate.now());
            init(fallback);
        }
    }

    public void init(InvertebrateStockEntryTo entry) {
        if (entry != null) {
            this.currentEntry = entry;
            if (this.currentEntry.getAquariumId() == null && userSession.getSelectedTank() != null) {
                this.currentEntry.setAquariumId(userSession.getSelectedTank().getId());
            }
            if (this.currentEntry.getAddedOn() == null) {
                this.currentEntry.setAddedOn(LocalDate.now());
            }
        }
    }

    public String onSave() {
        try {
            ResultTo<InvertebrateStockEntryTo> result;
            if (isEdit()) {
                result = invertebrateStockService.updateInvertebrate(currentEntry, userSession.getSabiBackendToken());
            } else {
                result = invertebrateStockService.addInvertebrate(currentEntry, userSession.getSabiBackendToken());
                if (result != null && result.getValue() != null) {
                    currentEntry.setId(result.getValue().getId());
                }
            }
            // Handle pending photo upload
            if (uploadedFile != null && currentEntry.getId() != null) {
                handlePhotoUpload();
            }
            invertebrateEntryNavContext.clear();
            return "/secured/tankEditor?faces-redirect=true";
        } catch (BusinessException e) {
            log.error("Failed to save invertebrate", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            return null;
        }
    }

    public String onCancel() {
        invertebrateEntryNavContext.clear();
        return "/secured/tankEditor?faces-redirect=true";
    }

    public void onPhotoUpload() {
        if (uploadedFile == null || uploadedFile.getContent() == null) return;
        previewPhoto = uploadedFile.getContent();
        if (currentEntry.getId() != null) {
            handlePhotoUpload();
        }
    }

    private void handlePhotoUpload() {
        if (currentEntry.getId() == null) {
            log.warn("Cannot upload invertebrate photo — no ID after save");
            return;
        }
        try {
            byte[] fileBytes = uploadedFile.getContent();
            String contentType = uploadedFile.getContentType() != null
                    ? uploadedFile.getContentType() : "image/jpeg";
            invertebrateStockService.uploadPhoto(currentEntry.getId(), fileBytes, contentType,
                    userSession.getSabiBackendToken());
            currentEntry.setHasPhoto(true);
            this.previewPhoto = fileBytes;
        } catch (BusinessException e) {
            log.error("Failed to upload photo for invertebrate {}", currentEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeletePhoto() {
        previewPhoto = null;
        currentEntry.setHasPhoto(false);
    }

    /** Catalogue search for autocomplete (min 2 chars). */
    public List<InvertebrateCatalogueSearchResultTo> onSearchCatalogue(String query) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        try {
            return invertebrateCatalogueService.search(query,
                    userSession.getLocale().getLanguage(),
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Invertebrate catalogue search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Called when user selects an entry from the catalogue autocomplete dropdown. */
    public void onSelectCatalogue(SelectEvent<InvertebrateCatalogueSearchResultTo> event) {
        InvertebrateCatalogueSearchResultTo result = event.getObject();
        if (result != null) {
            currentEntry.setScientificName(result.getScientificName());
            currentEntry.setInvertebrateCatalogueId(result.getId());
            if (result.getTaxonomicCategory() != null) {
                currentEntry.setTaxonomicCategory(result.getTaxonomicCategory());
            }
            if (result.getCareLevel() != null) {
                currentEntry.setCareLevel(result.getCareLevel());
            }
        }
    }

    /** Remove the catalogue link from the current entry. */
    public void onRemoveCatalogueLink() {
        if (currentEntry.getId() != null) {
            try {
                invertebrateStockService.removeCatalogueLink(currentEntry.getId(), userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                log.warn("Failed to remove catalogue link: {}", e.getMessage());
            }
        }
        currentEntry.setInvertebrateCatalogueId(null);
        currentEntry.setScientificName(null);
    }
}
