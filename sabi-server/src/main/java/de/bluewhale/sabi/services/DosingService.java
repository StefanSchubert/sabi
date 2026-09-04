/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.ResultTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages separate manual and automated aquarium dosing records.
 */
public interface DosingService {

    @NotNull
    List<DosingTo> listDosingsForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<DosingTo> createDosing(@NotNull Long aquariumId, @NotNull DosingTo dosingTo, @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<DosingTo> updateDosing(@NotNull Long aquariumId, @NotNull Long dosingId,
                                    @NotNull DosingTo dosingTo, @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<DosingTo> deleteDosing(@NotNull Long aquariumId, @NotNull Long dosingId, @NotNull String userEmail);
}
