/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralCatalogueService;
import de.bluewhale.sabi.webclient.apigateway.CoralStockService;
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

import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSF CDI-Bean controller for the coral stock entry form (add/edit).
 * Handles catalogue search, photo upload and inline growth/polyp history.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Getter
@Setter
@Slf4j
public class CoralStockEntryView implements Serializable {

    @Autowired
    CoralStockService coralStockService;

    @Autowired
    CoralCatalogueService coralCatalogueService;

    @Inject
    UserSession userSession;

    @Inject
    CoralEntryNavContext coralEntryNavContext;

    private CoralStockEntryTo currentEntry = new CoralStockEntryTo();
    private byte[] previewPhoto;
    private UploadedFile uploadedFile;

    // --- Inline Growth History ---
    private List<CoralGrowthHistoryTo> growthHistory = new ArrayList<>();
    private LocalDate newGrowthDate = LocalDate.now();
    private CoralGrowthType newGrowthType = CoralGrowthType.SIZE_CM;
    private BigDecimal newGrowthValue;

    // --- Inline Polyp Condition History ---
    private List<CoralPolypConditionTo> polypHistory = new ArrayList<>();
    private LocalDate newPolypDate = LocalDate.now();
    private PolypCondition newPolypCondition = PolypCondition.VITAL;

    /** All available coral classifications for the dropdown. */
    public CoralClassification[] getClassifications() {
        return CoralClassification.values();
    }

    /** All available growth types for the dropdown. */
    public CoralGrowthType[] getGrowthTypes() {
        return CoralGrowthType.values();
    }

    /** All available polyp conditions for the dropdown. */
    public PolypCondition[] getPolypConditions() {
        return PolypCondition.values();
    }

    /** All available care levels for the dropdown. */
    public CoralCareLevel[] getCareLevels() {
        return CoralCareLevel.values();
    }

    /** Derived: true = update, false = add. */
    public boolean isEdit() { return currentEntry != null && currentEntry.getId() != null; }

    @PostConstruct
    public void postConstruct() {
        CoralStockEntryTo ctxEntry = coralEntryNavContext.getEntry();
        if (ctxEntry != null && ctxEntry.getId() != null) {
            // Edit mode: reload fresh from backend
            try {
                CoralStockEntryTo fresh = coralStockService.getCoralById(
                        ctxEntry.getId(), userSession.getSabiBackendToken());
                if (fresh != null) {
                    init(fresh);
                } else {
                    init(ctxEntry);
                }
            } catch (BusinessException e) {
                log.warn("Could not reload coral {} from backend, using NavContext data", ctxEntry.getId(), e);
                init(ctxEntry);
            }
            // Load observation histories for inline display
            loadObservationHistories(ctxEntry.getId());
        } else if (ctxEntry != null) {
            init(ctxEntry);
        } else {
            CoralStockEntryTo fallback = new CoralStockEntryTo();
            fallback.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            fallback.setAddedOn(LocalDate.now());
            init(fallback);
        }
    }

    private void loadObservationHistories(Long coralId) {
        if (coralId == null) return;
        try {
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Could not load growth history for coral {}", coralId, e);
        }
        try {
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Could not load polyp history for coral {}", coralId, e);
        }
    }

    public void init(CoralStockEntryTo entry) {
        if (entry != null) {
            this.currentEntry = entry;
            if (this.currentEntry.getAquariumId() == null && userSession.getSelectedTank() != null) {
                this.currentEntry.setAquariumId(userSession.getSelectedTank().getId());
            }
            if (this.currentEntry.getAddedOn() == null) {
                this.currentEntry.setAddedOn(LocalDate.now());
            }
        } else {
            this.currentEntry = new CoralStockEntryTo();
            this.currentEntry.setAquariumId(
                    userSession.getSelectedTank() != null ? userSession.getSelectedTank().getId() : null);
            this.currentEntry.setAddedOn(LocalDate.now());
        }
    }

    public void onSave() {
        if (currentEntry.getAddedOn() != null && currentEntry.getAddedOn().isAfter(LocalDate.now())) {
            MessageUtil.error(null, "coralstock.form.entrydate.future", userSession.getLocale());
            return;
        }
        // Recover aquariumId from session if missing
        if (currentEntry.getAquariumId() == null && userSession.getSelectedTank() != null) {
            currentEntry.setAquariumId(userSession.getSelectedTank().getId());
        }
        boolean effectiveEdit = currentEntry.getId() != null;
        try {
            if (effectiveEdit) {
                coralStockService.updateCoral(currentEntry, userSession.getSabiBackendToken());
            } else {
                ResultTo<CoralStockEntryTo> result = coralStockService.addCoral(
                        currentEntry, userSession.getSabiBackendToken());
                if (result != null && result.getValue() != null && result.getValue().getId() != null) {
                    currentEntry.setId(result.getValue().getId());
                }
            }
        } catch (BusinessException e) {
            log.error("Failed to save coral entry", e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
            PrimeFaces.current().ajax().addCallbackParam("savedCoralId", 0L);
            return;
        }

        // Pass the saved coral ID back to the oncomplete JS callback
        long id = currentEntry.getId() != null ? currentEntry.getId() : 0L;
        PrimeFaces.current().ajax().addCallbackParam("savedCoralId", id);

        processPhotoUpload();
    }

    /** Catalogue search for autocomplete (min 2 chars). */
    public List<CoralCatalogueSearchResultTo> onSearchCatalogue(String query) {
        if (query == null || query.length() < 2) return Collections.emptyList();
        try {
            return coralCatalogueService.search(query,
                    userSession.getLocale().getLanguage(),
                    userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.warn("Coral catalogue search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Called when user selects a catalogue entry from the autocomplete dropdown. */
    public void onSelectCatalogue(SelectEvent<CoralCatalogueSearchResultTo> event) {
        CoralCatalogueSearchResultTo result = event.getObject();
        if (result != null) {
            currentEntry.setScientificName(result.getScientificName());
            currentEntry.setExternalRefUrl(result.getReferenceUrl());
            currentEntry.setCoralCatalogueId(result.getId());
            if (result.getClassification() != null) {
                currentEntry.setClassification(result.getClassification());
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
                coralStockService.removeCatalogueLink(currentEntry.getId(), userSession.getSabiBackendToken());
            } catch (BusinessException e) {
                log.warn("Failed to remove catalogue link: {}", e.getMessage());
            }
        }
        currentEntry.setCoralCatalogueId(null);
        currentEntry.setScientificName(null);
    }

    private void processPhotoUpload() {
        if (uploadedFile == null || uploadedFile.getContent() == null || uploadedFile.getSize() == 0) return;
        if (uploadedFile.getSize() > 5_242_880) {
            MessageUtil.error(null, "coralstock.form.photo.too_large", userSession.getLocale());
            return;
        }
        if (currentEntry.getId() == null) {
            log.warn("Cannot upload coral photo — no ID after save");
            return;
        }
        try {
            byte[] fileBytes = uploadedFile.getContent();
            String contentType = uploadedFile.getContentType() != null
                    ? uploadedFile.getContentType() : "image/jpeg";
            coralStockService.uploadPhoto(currentEntry.getId(), fileBytes, contentType,
                    userSession.getSabiBackendToken());
            currentEntry.setHasPhoto(true);
            this.previewPhoto = fileBytes;
        } catch (BusinessException e) {
            log.error("Failed to upload photo for coral {}", currentEntry.getId(), e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeletePhoto() {
        previewPhoto = null;
        currentEntry.setHasPhoto(false);
    }

    // ─── Growth History ──────────────────────────────────────────────────────

    public void onAddGrowthRecord() {
        Long coralId = currentEntry.getId();
        if (coralId == null || newGrowthType == null || newGrowthValue == null) {
            MessageUtil.error(null, "coralstock.growth.form.value.required", userSession.getLocale());
            return;
        }
        CoralGrowthHistoryTo rec = new CoralGrowthHistoryTo();
        rec.setCoralStockEntryId(coralId);
        rec.setMeasuredOn(newGrowthDate != null ? newGrowthDate : LocalDate.now());
        rec.setMeasurementType(newGrowthType);
        rec.setMeasurementValue(newGrowthValue);
        try {
            coralStockService.addGrowthRecord(coralId, rec, userSession.getSabiBackendToken());
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
            newGrowthDate = LocalDate.now();
            newGrowthValue = null;
        } catch (BusinessException e) {
            log.error("Failed to add growth record for coral {}", coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeleteGrowthRecord(CoralGrowthHistoryTo rec) {
        Long coralId = currentEntry.getId();
        if (coralId == null || rec == null) return;
        try {
            coralStockService.deleteGrowthRecord(coralId, rec.getId(), userSession.getSabiBackendToken());
            // Reload from backend (consistent with onAddGrowthRecord pattern).
            // Cannot use List.remove() directly because getGrowthHistory returns Arrays.asList()
            // which is a fixed-size list that does not support remove().
            growthHistory = coralStockService.getGrowthHistory(coralId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to delete growth record {} for coral {}", rec.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    // ─── Polyp Condition ─────────────────────────────────────────────────────

    public void onAddPolypObservation() {
        Long coralId = currentEntry.getId();
        if (coralId == null || newPolypCondition == null) {
            MessageUtil.error(null, "coralstock.polyp.form.condition.required", userSession.getLocale());
            return;
        }
        CoralPolypConditionTo obs = new CoralPolypConditionTo();
        obs.setCoralStockEntryId(coralId);
        obs.setObservedOn(newPolypDate != null ? newPolypDate : LocalDate.now());
        obs.setCondition(newPolypCondition);
        try {
            coralStockService.addPolypObservation(coralId, obs, userSession.getSabiBackendToken());
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
            newPolypDate = LocalDate.now();
            newPolypCondition = PolypCondition.VITAL;
        } catch (BusinessException e) {
            log.error("Failed to add polyp observation for coral {}", coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }

    public void onDeletePolypObservation(CoralPolypConditionTo obs) {
        Long coralId = currentEntry.getId();
        if (coralId == null || obs == null) return;
        try {
            coralStockService.deletePolypObservation(coralId, obs.getId(), userSession.getSabiBackendToken());
            // Reload from backend (consistent with onAddPolypObservation pattern).
            // Cannot use List.remove() directly because getPolypHistory returns Arrays.asList()
            // which is a fixed-size list that does not support remove().
            polypHistory = coralStockService.getPolypHistory(coralId, userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            log.error("Failed to delete polyp observation {} for coral {}", obs.getId(), coralId, e);
            MessageUtil.error(null, "common.error.backend_unreachable.l", userSession.getLocale());
        }
    }
}
