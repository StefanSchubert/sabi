/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.model.WaterChangeTo;

import java.io.Serializable;

/**
 * Gateway for the per-aquarium maintenance data: regular water exchange interval, recorded
 * water change events and the activated carbon setting.
 */
public interface MaintenanceService extends Serializable {

    MaintenanceTo getMaintenanceForTank(Long aquariumId, String token) throws BusinessException;

    MaintenanceProfileTo updateWaterChangeInterval(Long aquariumId, WaterChangeIntervalTo interval, String token)
            throws BusinessException;

    MaintenanceProfileTo updateActivatedCarbon(Long aquariumId, ActivatedCarbonTo carbon, String token)
            throws BusinessException;

    WaterChangeTo createWaterChange(Long aquariumId, WaterChangeTo waterChange, String token) throws BusinessException;

    WaterChangeTo updateWaterChange(Long aquariumId, Long recordId, WaterChangeTo waterChange, String token)
            throws BusinessException;

    void deleteWaterChange(Long aquariumId, Long recordId, String token) throws BusinessException;
}
