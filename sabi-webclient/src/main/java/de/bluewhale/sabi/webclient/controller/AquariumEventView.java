/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.AquariumEventService;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * CDI bean for the Aquarium Event Logbook panel in tankView.xhtml.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
@Getter
@Setter
public class AquariumEventView implements Serializable {

    @Inject
    UserSession userSession;

    @Inject
    TankListView tankListView;

    @Autowired
    AquariumEventService aquariumEventService;

    /** Map: aquariumId → list of events (loaded in @PostConstruct). */
    private Map<Long, List<AquariumEventTo>> eventsByTank = new LinkedHashMap<>();

    /** The event currently being edited/created in the inline form, per tank. */
    private Map<Long, AquariumEventTo> editFormByTank = new LinkedHashMap<>();

    /** Map: aquariumId → tank description (for display in the combined event list). */
    private Map<Long, String> tankDescriptionById = new LinkedHashMap<>();

    /**
     * Hidden form field: preserves the ID of the event currently being edited across
     * @RequestScope boundaries. Empty string = new record, non-empty = update.
     * Uses String (not Long) to avoid JSF empty-string → null-Long conversion issues.
     * getter: computed from editFormByTank (for rendering)
     * setter: called by JSF when form is submitted (for saveEvent)
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private String currentEventIdStr = "";

    /**
     * Hidden form field: preserves the optimistic-lock version for update requests.
     * getter: computed from editFormByTank (for rendering)
     * setter: called by JSF when form is submitted (for saveEvent)
     */
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private long currentEventOptlock = 0L;

    /**
     * Currently selected aquarium ID – bound to the p:selectOneMenu in tankView.xhtml.
     * Defaults to the first tank's ID on initial page load.
     * On AJAX-POSTs JSF sets this field from the form value before action methods are called.
     */
    private Long selectedAquariumId;

    @PostConstruct
    public void init() {
        List<AquariumTo> activeTanks = tankListView.getTanks();
        if (activeTanks == null) return;
        for (AquariumTo tank : activeTanks) {
            tankDescriptionById.put(tank.getId(), tank.getDescription());
            try {
                List<AquariumEventTo> events =
                    aquariumEventService.listEventsForTank(tank.getId(), userSession.getSabiBackendToken());
                // Wrap in new ArrayList: service returns Arrays.asList() (fixed-size, no add() support)
                eventsByTank.put(tank.getId(), events != null ? new ArrayList<>(events) : new ArrayList<>());
                editFormByTank.put(tank.getId(), new AquariumEventTo());
            } catch (BusinessException e) {
                log.error("Could not load events for tank_id={}: {}", tank.getId(), e.getMessage());
                eventsByTank.put(tank.getId(), new ArrayList<>());
                editFormByTank.put(tank.getId(), new AquariumEventTo());
            }
        }
        // Default selection: first tank (overwritten by JSF on AJAX-POSTs)
        if (selectedAquariumId == null && !eventsByTank.isEmpty()) {
            selectedAquariumId = eventsByTank.keySet().iterator().next();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Hidden-field getters/setters (custom – Lombok suppressed for these fields)
    // ────────────────────────────────────────────────────────────────────────

    /** Render-time getter: returns the edit form's id as String for the hidden input. */
    public String getCurrentEventIdStr() {
        AquariumEventTo f = (selectedAquariumId != null) ? editFormByTank.get(selectedAquariumId) : null;
        return (f != null && f.getId() != null) ? f.getId().toString() : "";
    }

    /** Submit-time setter: stores submitted hidden value for use in saveEvent(). */
    public void setCurrentEventIdStr(String v) {
        this.currentEventIdStr = (v != null) ? v.trim() : "";
    }

    /** Render-time getter: returns the edit form's optlock for the hidden input. */
    public long getCurrentEventOptlock() {
        AquariumEventTo f = (selectedAquariumId != null) ? editFormByTank.get(selectedAquariumId) : null;
        return (f != null) ? f.getOptlock() : 0L;
    }

    /** Submit-time setter: stores submitted hidden value for use in saveEvent(). */
    public void setCurrentEventOptlock(long v) {
        this.currentEventOptlock = v;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Methods using selectedAquariumId (used by the new single-form XHTML)
    // ────────────────────────────────────────────────────────────────────────

    /** Returns events for the currently selected aquarium (newest first). */
    public List<AquariumEventTo> getSelectedEvents() {
        if (selectedAquariumId == null) return Collections.emptyList();
        return eventsByTank.getOrDefault(selectedAquariumId, Collections.emptyList());
    }

    /**
     * Returns ALL events across all aquariums, sorted by date descending (newest first).
     * Used by the event list table – shows a combined view with Aquarium column.
     */
    public List<AquariumEventTo> getAllEvents() {
        return eventsByTank.values().stream()
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(AquariumEventTo::getEventDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }

    /** Returns the active edit/create form for the currently selected aquarium. */
    public AquariumEventTo getSelectedEditForm() {
        if (selectedAquariumId == null) return new AquariumEventTo();
        return editFormByTank.computeIfAbsent(selectedAquariumId, id -> new AquariumEventTo());
    }

    /**
     * Called when the aquarium dropdown changes (p:ajax listener).
     * Resets any ongoing edit so the form starts fresh for the newly selected tank.
     */
    public void onAquariumChange() {
        if (selectedAquariumId != null) {
            editFormByTank.put(selectedAquariumId, new AquariumEventTo());
        }
    }

    /**
     * Saves the event form for the currently selected aquarium (create or update).
     * ID and optlock are restored from hidden form fields (currentEventIdStr / currentEventOptlock)
     * since @RequestScope resets the editFormByTank map on every request.
     */
    public void saveEvent() {
        if (selectedAquariumId == null) return;
        AquariumEventTo form = getSelectedEditForm();
        // Restore id and optlock from hidden fields – these survive the @RequestScope boundary
        if (currentEventIdStr != null && !currentEventIdStr.isBlank()) {
            try {
                form.setId(Long.parseLong(currentEventIdStr));
                form.setOptlock(currentEventOptlock);
            } catch (NumberFormatException e) {
                log.warn("Invalid editingEventId in hidden field: {}", currentEventIdStr);
            }
        }
        form.setAquariumId(selectedAquariumId);
        try {
            if (form.getId() == null) {
                AquariumEventTo created = aquariumEventService.createEvent(
                    selectedAquariumId, form, userSession.getSabiBackendToken());
                List<AquariumEventTo> list = eventsByTank.computeIfAbsent(selectedAquariumId, id -> new ArrayList<>());
                list.add(0, created);
            } else {
                AquariumEventTo updated = aquariumEventService.updateEvent(
                    selectedAquariumId, form.getId(), form, userSession.getSabiBackendToken());
                List<AquariumEventTo> list = eventsByTank.get(selectedAquariumId);
                if (list != null) {
                    list.replaceAll(e -> e.getId().equals(updated.getId()) ? updated : e);
                }
            }
            editFormByTank.put(selectedAquariumId, new AquariumEventTo());
            MessageUtil.info(null, "aquariumevent.save.success.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not save event for tank_id={}: {}", selectedAquariumId, e.getMessage());
            MessageUtil.error(null, "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    /**
     * Populates the edit form for the given event.
     * Also switches selectedAquariumId to the event's aquarium so the correct
     * tank is pre-selected in the dropdown when editing from the combined list.
     */
    public void startEditEvent(AquariumEventTo event) {
        // Switch dropdown to the event's aquarium (important for combined list view)
        selectedAquariumId = event.getAquariumId();
        AquariumEventTo copy = new AquariumEventTo();
        copy.setId(event.getId());
        copy.setAquariumId(event.getAquariumId());
        copy.setEventDate(event.getEventDate());
        copy.setDurationHours(event.getDurationHours());
        copy.setDescription(event.getDescription());
        copy.setOptlock(event.getOptlock());
        editFormByTank.put(selectedAquariumId, copy);
    }

    /**
     * Permanently deletes the given event.
     * aquariumId is passed explicitly so it works from the combined list view
     * regardless of which aquarium is currently selected in the dropdown.
     */
    public void deleteEvent(Long aquariumId, Long eventId) {
        try {
            aquariumEventService.deleteEvent(aquariumId, eventId, userSession.getSabiBackendToken());
            List<AquariumEventTo> list = eventsByTank.get(aquariumId);
            if (list != null) list.removeIf(e -> e.getId().equals(eventId));
            MessageUtil.info(null, "aquariumevent.delete.success.t", userSession.getLocale());
        } catch (BusinessException e) {
            log.error("Could not delete event_id={} for tank_id={}: {}", eventId, aquariumId, e.getMessage());
            MessageUtil.error(null, "common.error.internal_server_problem.t", userSession.getLocale());
        }
    }

    /** Resets the edit form for the currently selected aquarium (New Entry button). */
    public void resetForm() {
        if (selectedAquariumId != null) {
            editFormByTank.put(selectedAquariumId, new AquariumEventTo());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Legacy methods (kept for backward compatibility with existing code paths)
    // ────────────────────────────────────────────────────────────────────────

    /** @deprecated Use getSelectedEvents() instead. */
    public List<AquariumEventTo> getEventsForTank(Long aquariumId) {
        return eventsByTank.getOrDefault(aquariumId, Collections.emptyList());
    }

    /** @deprecated Use getSelectedEditForm() instead. */
    public AquariumEventTo getEditFormForTank(Long aquariumId) {
        return editFormByTank.computeIfAbsent(aquariumId, id -> new AquariumEventTo());
    }
}
