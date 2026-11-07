/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceEventTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the per-aquarium maintenance data: regular water exchange interval, recorded
 * maintenance events (water changes and carbon replacements) and the activated carbon setting.
 * Every operation is ownership-checked; a missing or foreign aquarium yields an ERROR result
 * (mapped to 403 by the controller).
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
    ResultTo<MaintenanceEventTo> createMaintenanceEvent(@NotNull Long aquariumId,
                                                        @NotNull MaintenanceEventTo eventTo,
                                                        @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<MaintenanceEventTo> updateMaintenanceEvent(@NotNull Long aquariumId, @NotNull Long eventId,
                                                        @NotNull MaintenanceEventTo eventTo,
                                                        @NotNull String userEmail);

    @Transactional
    @NotNull
    ResultTo<MaintenanceEventTo> deleteMaintenanceEvent(@NotNull Long aquariumId, @NotNull Long eventId,
                                                        @NotNull String userEmail);
}
