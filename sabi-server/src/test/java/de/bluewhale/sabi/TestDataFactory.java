/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Small Util class which provide common test DTOs.
 *
 * @author Stefan Schubert
 */
public class TestDataFactory {
// ------------------------------ FIELDS ------------------------------

    public static final String TESTUSER_EMAIL1 = "testservice1@bluewhale.de";
    public static final String TESTUSER_EMAIL2 = "testservice2@bluewhale.de";

    public static final String INVALID_PASSWORD = "quertz!1";
    public static final String VALID_PASSWORD = "All!Rules8-)Applied";

    public static final Long TEST_TANK_ID = 1L; // do not change - integration tests rely on this
    public static final Long TEST_USER_ID = 1L; // do not change - integration tests rely on this

    private static TestDataFactory instance;


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

    public AquariumTo getTestAquariumTo() {
        final AquariumTo aquariumTo = new AquariumTo();
        aquariumTo.setDescription("Test Tank");
        aquariumTo.setActive(Boolean.TRUE);
        aquariumTo.setSize(40);
        aquariumTo.setSizeUnit(SizeUnit.LITER);
        aquariumTo.setWaterType(WaterType.SEA_WATER);
        aquariumTo.setId(TEST_TANK_ID);
        return aquariumTo;
    }

    public AquariumTo getTestAquariumFor(UserTo userTo) {
        AquariumTo aquariumTo = getTestAquariumTo();
        aquariumTo.setUserId(userTo.getId());
        return aquariumTo;
    }

    public AquariumEntity getTestAquariumEntity(AquariumTo aquariumTo, UserEntity userEntity) {
        AquariumEntity aquariumEntity = new AquariumEntity();
        aquariumEntity.setId(aquariumTo.getId());
        aquariumEntity.setDescription(aquariumTo.getDescription());
        aquariumEntity.setSize(aquariumTo.getSize());
        aquariumEntity.setSizeUnit(aquariumTo.getSizeUnit());
        aquariumEntity.setWaterType(aquariumTo.getWaterType());
        aquariumEntity.setUser(userEntity);
        return aquariumEntity;
    }



    public UserTo getNewTestUserTo(String eMail) {
        UserTo userTo1 = new UserTo(eMail, eMail, VALID_PASSWORD);
        userTo1.setLanguage(Locale.ENGLISH.getLanguage());
        userTo1.setCountry(Locale.UK.getCountry());
        userTo1.setValidated(true);
        userTo1.setId(TEST_USER_ID);
        return userTo1;
    }

    public UserEntity getNewTestUserEntity(UserTo userTo) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(userTo.getEmail());
        userEntity.setId(userTo.getId());
        userEntity.setLanguage(userTo.getLanguage());
        userEntity.setCountry(userTo.getCountry());
        userEntity.setValidated(userTo.isValidated());
        return userEntity;
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
     * @param aquariumTo, can be null in which case we assign a default tank id
     * @return
     */
    public MeasurementTo getTestMeasurementTo(AquariumTo aquariumTo) {
        final MeasurementTo measurementTo = new MeasurementTo();
        measurementTo.setAquariumId(aquariumTo == null ? TEST_TANK_ID:aquariumTo.getId());
        measurementTo.setUnitId(1);
        measurementTo.setId(4711l);
        measurementTo.setMeasuredValue(1.15f);
        measurementTo.setMeasuredOn(LocalDateTime.now());
        return measurementTo;
    }

    public MeasurementEntity getTestMeasurementEntity(MeasurementTo measurementTo, AquariumEntity aquariumEntity) {
        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setId(measurementTo.getId());
        measurementEntity.setUnitId(measurementTo.getUnitId());
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setMeasuredOn(measurementTo.getMeasuredOn());
        measurementEntity.setMeasuredValue(measurementTo.getMeasuredValue());
        return measurementEntity;
    }

    /**
     * Will assign Default testuser and test-aquarium &
     * @return a valid MeasurementEntity
     */
    public MeasurementEntity getTestMeasurementEntityWithDefaults() {
        UserTo testUserTo = getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity newTestUserEntity = getNewTestUserEntity(testUserTo);
        AquariumTo testAquariumTo = getTestAquariumTo();
        AquariumEntity testAquariumEntity = getTestAquariumEntity(testAquariumTo, newTestUserEntity);
        MeasurementTo testMeasurementTo = getTestMeasurementTo(testAquariumTo);
        return getTestMeasurementEntity(testMeasurementTo, testAquariumEntity);
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
