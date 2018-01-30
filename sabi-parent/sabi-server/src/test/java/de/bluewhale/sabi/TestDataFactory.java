/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;

import java.time.LocalDateTime;

/**
 * Small Util class which provide common test data.
 *
 * @author Stefan Schubert
 */
public class TestDataFactory {
// ------------------------------ FIELDS ------------------------------

    public static final String TESTUSER_EMAIL = "testservice@bluewhale.de";

    private static TestDataFactory instance;

    private TankService tankService;
    private UserService userService;

// -------------------------- STATIC METHODS --------------------------

    protected TestDataFactory() {
        // defeat instantiation from outside
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public static TestDataFactory getInstance() {
        if (instance == null) {
            instance = new TestDataFactory();
        }
        return instance;
    }

// -------------------------- OTHER METHODS --------------------------

    // FIXME: 14.06.17 Add singelton constructor
    public AquariumTo getTestAquariumTo() {
        final AquariumTo aquariumTo = new AquariumTo();
        aquariumTo.setDescription("Test Tank");
        aquariumTo.setActive(Boolean.TRUE);
        aquariumTo.setSize(40);
        aquariumTo.setSizeUnit(SizeUnit.LITER);
        return aquariumTo;
    }

    public AquariumTo getTestAquariumFor(UserTo userTo) {
        AquariumTo aquariumTo = getTestAquariumTo();
        aquariumTo.setId(1L);
        aquariumTo.setUserId(userTo.getId());
        return aquariumTo;
    }

    public UserTo getPersistedTestUserTo(String eMail) {
        String clearTextPassword = "NoPass123";
        final UserTo userTo = new UserTo(eMail, clearTextPassword);
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        return userToResultTo.getValue();
    }

    /**
     * Links preexisting testdata for aquariun (user) and unit.
     *
     * @return
     * @param pTankID
     */
    public MeasurementTo getTestMeasurementTo(Long pTankID) {
        final MeasurementTo measurementTo = new MeasurementTo();
        measurementTo.setAquariumId(pTankID);
        measurementTo.setUnitId(1);
        measurementTo.setMeasuredValue(15.5f);
        measurementTo.setMeasuredOn(LocalDateTime.now());
        return measurementTo;
    }

    public TestDataFactory withTankService(TankService service) {
        tankService = service;
        return this;
    }

    public TestDataFactory withUserService(UserService service) {
        userService = service;
        return this;
    }
}
