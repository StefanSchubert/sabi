/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.MeasurementService;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import de.bluewhale.sabi.webclient.utils.MessageUtil;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static de.bluewhale.sabi.webclient.utils.PageRegister.MEASUREMENT_VIEW_PAGE;

/**
 * Controller for the Measurement View as shown in measureView.xhtml
 *
 * @author Stefan Schubert
 */
@Named
@SessionScope
@Slf4j
@Getter
public class MeasurementListView extends AbstractControllerTools implements Serializable  {

    private static final int MAX_RESULT_COUNT = 5;

    @Autowired
    MeasurementService measurementService;

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    // View Modell
    private List<AquariumTo> tanks;
    private List<UnitTo> knownUnits;
    private List<MeasurementTo> measurementsTakenByUser = Collections.emptyList();
    private MeasurementTo measurement = new MeasurementTo();

    @PostConstruct
    public void init() {
        // user should be able to choose from his tanks
        try {
            tanks = tankService.getUsersTanks(userSession.getSabiBackendToken());
            if (tanks.size() == 1) {
                // default selection if user has only one tank
                measurement.setAquariumId(tanks.get(0).getId());
            }
        } catch (BusinessException e) {
            tanks = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg","common.token.expired.t",userSession.getLocale());
        }

        try {
            knownUnits = measurementService.getAvailableMeasurementUnits(userSession.getSabiBackendToken());
        } catch (BusinessException e) {
            knownUnits = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg","common.token.expired.t",userSession.getLocale());
        }

        fetchUsersLatestMeasurements();

    }

    private void fetchUsersLatestMeasurements() {
        try {
            measurementsTakenByUser = measurementService.getMeasurementsTakenByUser(userSession.getSabiBackendToken(), MAX_RESULT_COUNT);
        } catch (BusinessException e) {
            measurementsTakenByUser = Collections.emptyList();
            log.error(e.getLocalizedMessage());
            MessageUtil.error("troubleMsg","common.token.expired.t",userSession.getLocale());
        }
    }

    /**
     * Used to request the Unitsign, when all you have is a reference Id.
     *
     * @param unitId technical key of the Unit.
     * @return "N/A" if unitId is unknown
     */
    @NotNull
    public String getUnitSignForId(Integer unitId) {
        return getUnitSignForId(unitId,knownUnits);
    }

    /**
     * Used to request the TankName, when all you have is a reference Id.
     *
     * @param tankId technical key of the Tank.
     * @return "N/A" if tankId is unknown
     */
    @NotNull
    public String getTankNameForId(Long tankId) {
        return getTankNameForId(tankId,tanks);
    }


    public String resetForm() {
        measurement = new MeasurementTo();
        return MEASUREMENT_VIEW_PAGE.getNavigationableAddress();
    }

    public String save() {
        if (allDataProvided(measurement)) {
            try {
                measurementService.save(measurement, userSession.getSabiBackendToken());
                fetchUsersLatestMeasurements();
                MessageUtil.info("submitState", "common.save.confirmation.t", userSession.getLocale());
            } catch (Exception e) {
                log.error("Couldn't save measurement {} {}",measurement, e);
                MessageUtil.error("submitState", "common.error.internal_server_problem.t", userSession.getLocale());
            }
        } else {
            MessageUtil.warn("troubleMsg", "common.incompleted_formdata.t", userSession.getLocale());
        }
        return MEASUREMENT_VIEW_PAGE.getNavigationableAddress();
    }

    private boolean allDataProvided(MeasurementTo measurement) {
        boolean result = true;
        if (measurement.getMeasuredOn() == null) result = false;
        if (measurement.getAquariumId() == null) result = false;
        if (measurement.getUnitId() == 0) result = false;
        log.debug("allDataProvided = {}, Object was {}",result,measurement);
        return result;
    }

    public void editMeasurement(MeasurementTo existingMeasurement) {
        measurement = existingMeasurement;
    }

    public String getGetDescriptionFor(Integer unitId) {
        return getUnitDescriptionForId(unitId, knownUnits);
    }

    public String getGetThresholdInfoFor(Integer unitId) {
        String result = "";
        if (unitId != null && unitId != 0) {
            try {
                ParameterTo parameterTo = measurementService.getParameterFor(unitId, userSession.getSabiBackendToken());
                Locale usersLocale = userSession.getLocale();
                if (parameterTo != null && usersLocale != null) {
                    result = String.format(usersLocale, "min = %.03f / max = %.03f", parameterTo.getMinThreshold(), parameterTo.getMaxThreshold());
                }
            } catch (Exception e) {
                log.warn("Could not resolve parameter info for unit {}", unitId);
            }
        }
        return result;
    }
}
