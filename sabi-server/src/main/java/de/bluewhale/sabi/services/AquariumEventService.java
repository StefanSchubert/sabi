/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing aquarium logbook events.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
public interface AquariumEventService {

    /**
     * Returns all events for the given tank (newest first).
     * Verifies that aquariumId belongs to userEmail before returning data; returns empty list on mismatch.
     *
     * @param aquariumId  tank PK
     * @param userEmail   authenticated user (ownership check)
     * @return list of events, never null
     */
    @NotNull
    List<AquariumEventTo> listEventsForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    /**
     * Creates a new event for the given tank.
     * Returns HTTP-relevant result; the aquarium ownership is verified before insert.
     *
     * @param aquariumId  tank PK
     * @param eventTo     event data (id should be null)
     * @param userEmail   authenticated user
     * @return ResultTo wrapping the persisted DTO with its generated id on INFO, ERROR on ownership fail
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> createEvent(@NotNull Long aquariumId,
                                          @NotNull AquariumEventTo eventTo,
                                          @NotNull String userEmail);

    /**
     * Updates an existing event.
     * Verifies: (a) event.aquariumId == aquariumId, (b) aquarium belongs to userEmail.
     *
     * @param aquariumId  tank PK
     * @param eventId     event PK
     * @param eventTo     updated data (must include optlock for optimistic locking)
     * @param userEmail   authenticated user
     * @return ResultTo wrapping the updated DTO on INFO, ERROR on ownership/not-found
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> updateEvent(@NotNull Long aquariumId,
                                          @NotNull Long eventId,
                                          @NotNull AquariumEventTo eventTo,
                                          @NotNull String userEmail);

    /**
     * Permanently deletes an event.
     * Verifies: (a) event.aquariumId == aquariumId, (b) aquarium belongs to userEmail.
     *
     * @param aquariumId  tank PK
     * @param eventId     event PK
     * @param userEmail   authenticated user
     * @return INFO on success, ERROR on ownership/not-found
     */
    @Transactional
    @NotNull
    ResultTo<AquariumEventTo> deleteEvent(@NotNull Long aquariumId,
                                          @NotNull Long eventId,
                                          @NotNull String userEmail);
}

