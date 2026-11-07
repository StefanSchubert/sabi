/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.MaintenanceEventEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for recorded maintenance events (water changes and carbon replacements).
 */
public interface MaintenanceEventRepository extends JpaRepository<MaintenanceEventEntity, Long> {

    @NotNull
    List<MaintenanceEventEntity> findByAquariumIdOrderByChangedOnDesc(@NotNull Long aquariumId);

    Optional<MaintenanceEventEntity> findByIdAndAquariumId(@NotNull Long id, @NotNull Long aquariumId);
}
