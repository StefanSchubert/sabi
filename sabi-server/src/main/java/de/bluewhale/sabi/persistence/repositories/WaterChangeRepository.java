/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.WaterChangeEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for recorded water change events.
 */
public interface WaterChangeRepository extends JpaRepository<WaterChangeEntity, Long> {

    @NotNull
    List<WaterChangeEntity> findByAquariumIdOrderByChangedOnDesc(@NotNull Long aquariumId);

    Optional<WaterChangeEntity> findByIdAndAquariumId(@NotNull Long id, @NotNull Long aquariumId);
}
