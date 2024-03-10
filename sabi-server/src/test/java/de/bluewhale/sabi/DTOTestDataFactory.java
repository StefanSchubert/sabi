/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.PlagueCenterService;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Small Util class which provide common test data.
 *
 * @author Stefan Schubert
 */
public class DTOTestDataFactory {
// ------------------------------ FIELDS ------------------------------

    public static final String TESTUSER_EMAIL1 = "testservice1@bluewhale.de";
    public static final String TESTUSER_EMAIL2 = "testservice2@bluewhale.de";

    public static final String INVALID_PASSWORD = "quertz!1";
    public static final String VALID_PASSWORD = "All!Rules8-)Applied";

    private static DTOTestDataFactory instance;

    private TankService tankService;
    private UserService userService;

    private PlagueCenterService plagueCenterService;

// -------------------------- STATIC METHODS --------------------------

    protected DTOTestDataFactory() {
        // defeat instantiation from outside
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public static DTOTestDataFactory getInstance() {
        if (instance == null) {
            instance = new DTOTestDataFactory();
        }
        return instance;
    }

// -------------------------- OTHER METHODS --------------------------

    public AquariumTo getTestAquariumTo() {
        final AquariumTo aquariumTo = new AquariumTo();
        aquariumTo.setDescription("Test Tank");
        aquariumTo.setActive(Boolean.TRUE);
        aquariumTo.setSize(40);
        aquariumTo.setSizeUnit(SizeUnit.LITER);
        aquariumTo.setWaterType(WaterType.SEA_WATER);
        return aquariumTo;
    }

    public AquariumTo getTestAquariumFor(UserTo userTo) {
        AquariumTo aquariumTo = getTestAquariumTo();
        aquariumTo.setId(1L);
        aquariumTo.setUserId(userTo.getId());
        aquariumTo.setWaterType(WaterType.SEA_WATER);
        return aquariumTo;
    }

    public UserTo getRegisterNewTestUser(String eMail) {
        String clearTextPassword = VALID_PASSWORD;
        final NewRegistrationTO userTo = new NewRegistrationTO(eMail, eMail, clearTextPassword);
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        return userToResultTo.getValue();
    }

    public UserProfileTo getBasicUserProfileTo() {
        return new UserProfileTo(Locale.ENGLISH.getLanguage(), Locale.UK.getCountry());
    }

    public UserProfileTo getUserProfileToWithMeasurementReminderFor(UserTo userTo) {
        UserProfileTo userProfileTo = getBasicUserProfileTo();
        MeasurementReminderTo reminderTo = new MeasurementReminderTo();
        reminderTo.setUserId(userTo.getId().intValue());
        reminderTo.setPastDays(12);
        reminderTo.setUnitId(1);
        userProfileTo.getMeasurementReminderTos().add(reminderTo);

        return userProfileTo;
    }


    /**
     * Links preexisting testdata for aquarium (user) and unit.
     *
     * @param pTankID
     * @return
     */
    public MeasurementTo getTestMeasurementTo(Long pTankID) {
        final MeasurementTo measurementTo = new MeasurementTo();
        measurementTo.setAquariumId(pTankID);
        measurementTo.setUnitId(1);
        measurementTo.setId(4711l);
        measurementTo.setMeasuredValue(1.15f);
        measurementTo.setMeasuredOn(LocalDateTime.now());
        return measurementTo;
    }

    public DTOTestDataFactory withTankService(TankService service) {
        tankService = service;
        return this;
    }

    public DTOTestDataFactory withUserService(UserService service) {
        userService = service;
        return this;
    }

    public DTOTestDataFactory withPlagueCenterService(PlagueCenterService service) {
        plagueCenterService = service;
        return this;
    }

    public ParameterTo getTestParameterTo() {
        ParameterTo parameterTo = new ParameterTo();
        parameterTo.setBelongingUnitId(4711);
        parameterTo.setMaxThreshold(10f);
        parameterTo.setMinThreshold(20f);
        parameterTo.setId(101);
        parameterTo.setDescription("Junit");
        return parameterTo;
    }

    public UnitTo getTestUnitTo() {
        UnitTo unitTo = new UnitTo();
        unitTo.setId(4711);
        unitTo.setUnitSign("SP");
        unitTo.setDescription("Scrum Story Points");
        return unitTo;
    }
}
