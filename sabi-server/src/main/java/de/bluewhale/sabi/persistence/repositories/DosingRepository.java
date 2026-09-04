/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.DosingEntity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for separate aquarium dosing records.
 */
public interface DosingRepository extends JpaRepository<DosingEntity, Long> {

    @NotNull
    List<DosingEntity> findByAquariumIdOrderByRecordedOnDesc(@NotNull Long aquariumId);

    Optional<DosingEntity> findByIdAndAquariumId(@NotNull Long id, @NotNull Long aquariumId);
}
