/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.model.WaterChangeTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the per-aquarium maintenance data: regular water exchange interval, recorded water
 * change events and the activated carbon setting. Every operation is ownership-checked; a
 * missing or foreign aquarium yields an ERROR result (mapped to 403 by the controller).
 */
public interface MaintenanceService {

    @NotNull
    ResultTo<MaintenanceTo> getMaintenanceForTank(@NotNull Long aquariumId, @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<MaintenanceProfileTo> updateWaterChangeInterval(@NotNull Long aquariumId,
                                                             @NotNull WaterChangeIntervalTo intervalTo,
                                                             @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<MaintenanceProfileTo> updateActivatedCarbon(@NotNull Long aquariumId,
                                                         @NotNull ActivatedCarbonTo carbonTo,
                                                         @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<WaterChangeTo> createWaterChange(@NotNull Long aquariumId,
                                              @NotNull WaterChangeTo waterChangeTo,
                                              @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<WaterChangeTo> updateWaterChange(@NotNull Long aquariumId, @NotNull Long recordId,
                                              @NotNull WaterChangeTo waterChangeTo,
                                              @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<WaterChangeTo> deleteWaterChange(@NotNull Long aquariumId, @NotNull Long recordId,
                                              @NotNull String userEmail);
}
