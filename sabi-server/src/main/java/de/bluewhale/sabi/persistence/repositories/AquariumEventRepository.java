/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.AquariumEventEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for aquarium logbook events.
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
public interface AquariumEventRepository extends JpaRepository<AquariumEventEntity, Long> {

    /**
     * Returns all events for a tank, newest first.
     * Used by the authenticated logbook view (no date filter).
     */
    @NotNull
    List<AquariumEventEntity> findByAquariumIdOrderByEventDateDesc(@NotNull Long aquariumId);

    /**
     * Returns events for a tank on or after the given cutoff date, newest first.
     * Used by the public report assembly (rolling 365-day window, FR-014).
     */
    @NotNull
    List<AquariumEventEntity> findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(
            @NotNull Long aquariumId, @NotNull LocalDate cutoff);

    /**
     * Ownership-safe single-event lookup.
     * Returns empty if the event does not exist or belongs to a different aquarium.
     * Used before update and delete to enforce FR-009.
     */
    Optional<AquariumEventEntity> findByIdAndAquariumId(@NotNull Long id, @NotNull Long aquariumId);
}

