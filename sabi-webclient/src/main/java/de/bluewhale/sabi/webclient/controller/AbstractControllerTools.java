/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.UnitTo;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Some Tools that are required in more than one controller, and that are not supposed to be double implemented.
 *
 * @author Stefan Schubert
 */
@Slf4j
public abstract class AbstractControllerTools {

    /**
     * Used to request the Unitsign, when all you have is a reference Id.
     *
     * @param unitId technical key of the Unit.
     * @return "N/A" if unitId is unknown
     */
    @NotNull
    public String getUnitSignForId(Integer unitId, List<UnitTo> fromUnitList) {
        // TODO STS (13.04.21): Improvment: instead of providing the list of known units those should be fetched here
        // wee need to change the auth scope on the api for this
        String result = "N/A";
        if (unitId != null) {
            for (UnitTo unitTo : fromUnitList) {
                if (unitTo.getId().equals(unitId)) {
                    result = unitTo.getUnitSign();
                    break;
                }
            }
        }
        if (result.equals("N/A")) {
            log.warn("Could not determine the unit sign for unitID: {}", unitId);
        }
        return result;
    }

    /**
     * Used to request the UnitDescription, when all you have is a reference Id.
     *
     * @param unitId technical key of the Unit.
     * @return "N/A" if unitId is unknown
     */
    @NotNull
    public String getUnitDescriptionForId(Integer unitId, List<UnitTo> fromUnitList) {
        // TODO STS (13.04.21): Improvement: instead of providing the list of known units those should be fetched here
        // we need to change the auth scope on the api for this
        String result = "";
        if (unitId != null) {
            for (UnitTo unitTo : fromUnitList) {
                if (unitTo.getId().equals(unitId)) {
                    result = unitTo.getDescription();
                    break;
                }
            }
        }
        if (result.equals("")) {
            log.warn("Could not determine the unit description for unitID: {}", unitId);
        }
        return result;
    }

    /**
     * Used to request the TankName, when all you have is a reference Id.
     *
     * @param tankId technical key of the Tank.
     * @return "N/A" if tankId is unknown
     */
    @NotNull
    public String getTankNameForId(Long tankId, List<AquariumTo> tanks) {
        String result = "N/A";
        if (tankId != null) {
            for (AquariumTo aquariumTo : tanks) {
                if (aquariumTo.getId().equals(tankId)) {
                    result = aquariumTo.getDescription();
                    break;
                }
            }
        }
        if (result.equals("N/A")) {
            log.warn("Could not determine the tankname for tankID: {}", tankId);
        }
        return result;
    }

}
