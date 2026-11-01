/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for the per-aquarium maintenance profile.
 */
public interface TankMaintenanceProfileRepository extends JpaRepository<TankMaintenanceProfileEntity, Long> {

    Optional<TankMaintenanceProfileEntity> findByAquariumId(@NotNull Long aquariumId);
}
