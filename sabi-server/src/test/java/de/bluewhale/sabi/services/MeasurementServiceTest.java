/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.BasicDataFactory;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.*;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;


/**
 * Business-Layer tests for MeasurementServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MeasurementServiceTest extends BasicDataFactory {


    // ------------------------------ FIELDS ------------------------------

    @Autowired
    private TankService tankService;

    @Autowired
    private UserService userService;

    @Autowired
    private MeasurementService measurementService;

// -------------------------- OTHER METHODS --------------------------

    /**
     * There seems to be a timing problem with H2, that causes, that the basic data is not available
     * for some test classes, while for others it worked out. Or the Annotation is not being processed through
     * inheritance. Until we know what's going wrong...
     * we "double inject" by extending the BasicTestDataFactory and by calling it directly.
     * The different behaviour can be observed by e.g. calling the master test suite and as comparising
     * the measurement testsuite while this is method is deaktivated.
     */
    @Before
    public void ensureBasicDataAvailability() {
        List<MeasurementTo> list = measurementService.listMeasurements(P_USER1_EMAIL, 0);
        if (list.isEmpty()) {
            populateBasicData();
            list = measurementService.listMeasurements(P_USER1_EMAIL, 0);
        }
        assertTrue("H2-Basicdata injection did not work!", list.size() > 0);

        // PROBLEM here: We thought that stores User gets the ID wie set hard, but H2 Dirties context
        // drops data but not sequence values, which is why repeated user creation leads to a different user ID.
        // So any test which is relies on a certain user ID might run into trouble.
        //        UserEntity storedTestUser = userRepository.getByEmail(P_USER1_EMAIL);
        //        assertNotNull("Precondition stored Test User failed!",storedTestUser);
        //        assertEquals("Stored Test User got wrong ID!? ", 1l, storedTestUser.getId().longValue());
    }

    @Test
    @Transactional
    public void testListMeasurements() throws Exception {
        // Given already stored testdata for measurements
        AquariumTo aquariumTo = tankService.listTanks(P_USER1_EMAIL).get(0);
        assertNotNull("Prepersisted Testdata is missing", aquariumTo);
        Long tankID = aquariumTo.getId();

        // When
        List<MeasurementTo> tank1Measurements = measurementService.listMeasurements(tankID);
        List<MeasurementTo> usersMeasurements = measurementService.listMeasurements(P_USER1_EMAIL, 0);

        // Then
        assertNotNull(tank1Measurements);
        assertNotNull(usersMeasurements);
        assertTrue("Testdata gone?", tank1Measurements.size() >= 1);
        assertTrue("Stored Testdata changed?", usersMeasurements.containsAll(tank1Measurements));
    }

    @Test
    @Transactional
    public void testFindMeasurementParameter() throws Exception {
        // Given already stored testdata for measurements

        // When
        ParameterTo parameterTo = measurementService.fetchParameterInfoFor(1);

        // Then
        assertNotNull(parameterTo);
    }

    @Test
    @Transactional
    public void testFindInvalidMeasurementParameter() throws Exception {
        // Given already stored testdata for measurements
        Integer nonExistingUnit = Integer.MAX_VALUE;

        // When
        ParameterTo parameterTo = measurementService.fetchParameterInfoFor(nonExistingUnit);

        // Then
        assertNull(parameterTo);
    }


    @Test
    @Transactional
    public void testListMeasurementsForSpecificTankAndUnit() throws Exception {
        // Given already stored testdata for measurements
        // for tank 2 only one measurement with unit id 1

        // When
        List<MeasurementTo> measurements = measurementService.listMeasurementsFilteredBy(2L, 1);

        // Then
        assertNotNull(measurements);
        assertTrue("Testdata gone or changed? Received more or less than expected on measurement.", measurements.size() == 1);
    }

    @Test
    @Transactional
    public void testListMeasurementUnits() throws Exception {
        // Given already stored testdata for measurements

        // When
        List<UnitTo> measurementUnits = measurementService.listAllMeasurementUnits();

        // Then
        assertNotNull(measurementUnits);
        assertTrue("Testdata gone?", measurementUnits.size() >= 1);
    }

    @Test
    @Transactional
    public void testAddNewMeasurement() throws Exception {
        // Given already store test data
        AquariumTo aquariumTo = tankService.listTanks(P_USER1_EMAIL).get(0);
        assertNotNull("Prepersisted Testdata is missing", aquariumTo);
        Long tankID = aquariumTo.getId();

        // When adding a new Measurment
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(tankID);
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addMeasurement(testMeasurementTo, P_USER1_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        assertEquals("Creating measurement failed? " + measurementToResultTo.getMessage().getCode(), CATEGORY.INFO, measurementToResultTo.getMessage().getType());
    }

    @Test
    @Transactional
    public void testGetLastetMeasurementEntryDateTime() throws Exception {

        // Given already store test data
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        AquariumTo aquariumTo = tankService.listTanks(P_USER1_EMAIL).get(0);
        assertNotNull("Prepersisted Testdata missing", aquariumTo);
        Long tankID = aquariumTo.getId();

        // Stored Measurement A
        MeasurementTo testMeasurementATo = testDataFactory.getTestMeasurementTo(tankID);
        testMeasurementATo.setMeasuredOn(LocalDateTime.now().minusYears(2));
        ResultTo<MeasurementTo> measurementAToResultTo = measurementService.addMeasurement(testMeasurementATo, P_USER1_EMAIL);
        assertEquals("Failure storing test data A?: " + measurementAToResultTo.getMessage().getCode(), CATEGORY.INFO, measurementAToResultTo.getMessage().getType());


        // And Afterwards created Measurement B
        MeasurementTo testMeasurementBTo = testDataFactory.getTestMeasurementTo(tankID);
        testMeasurementBTo.setId(testMeasurementATo.getId() + 1l);
        testMeasurementBTo.setMeasuredValue(99f);
        ResultTo<MeasurementTo> measurementBToResultTo = measurementService.addMeasurement(testMeasurementBTo, P_USER1_EMAIL);
        assertEquals("Failure storing test data B?: " + measurementBToResultTo.getMessage().getCode(), CATEGORY.INFO, measurementBToResultTo.getMessage().getType());

        // When
        LocalDateTime lastRecordedTime = measurementService.getLastTimeOfMeasurementTakenFilteredBy(measurementAToResultTo.getValue().getAquariumId(), testMeasurementATo.getUnitId());

        // Then
        assertNotNull("Looked like stored measurments have not been flushed.", lastRecordedTime);
        assertEquals("Did not retrieved the latest measurement date", LocalDateTime.now().getYear(), lastRecordedTime.getYear());
    }


    @Test
    @Transactional
    public void testRemoveMeasurement() throws Exception {
        // Given a stored measurement for a tank and user
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        testDataFactory.withUserService(userService);
        testDataFactory.withTankService(tankService);
        String newTestUserMail = "junit@sabi.de";
        UserTo persistedTestUserTo = testDataFactory.getRegisterNewTestUser(newTestUserMail);
        AquariumTo testAquariumTo = testDataFactory.getTestAquariumFor(persistedTestUserTo);
        testAquariumTo.setId(57654L);
        ResultTo<AquariumTo> newTankResultTo = tankService.registerNewTank(testAquariumTo, newTestUserMail);

        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(newTankResultTo.getValue().getId());
        testMeasurementTo.setId(889911L); // to ensure not to interfere with others
        ResultTo<MeasurementTo> measurementToResultTo1 = measurementService.addMeasurement(testMeasurementTo, newTestUserMail);

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.removeMeasurement(testMeasurementTo.getId(), newTestUserMail);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Removal of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }

    @Test
    @Transactional
    public void testUpdateMeasurement() throws Exception {
        // Given already stored measurements
        List<MeasurementTo> measurementToList = measurementService.listMeasurements(P_USER1_EMAIL, 0);
        MeasurementTo prestoresMeasurementTo = measurementToList.get(0);
        float oldValue = prestoresMeasurementTo.getMeasuredValue();

        // When we update the measurement
        float newValue = oldValue + 1.5f;
        prestoresMeasurementTo.setMeasuredValue(newValue);
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.updateMeasurement(prestoresMeasurementTo, P_USER1_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertEquals(newValue, measurementToResultTo.getValue().getMeasuredValue(), 0f);
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Update of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }


    @Test
    @Transactional
    public void testAddIotAuthorizedMeasurement() {
        // Given already store test data
        AquariumTo aquariumTo = tankService.listTanks(P_USER1_EMAIL).get(0);
        assertNotNull("Prepersisted Testdata is missing", aquariumTo);
        Long tankID = aquariumTo.getId();

        // When
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(tankID);
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addIotAuthorizedMeasurement(testMeasurementTo);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Creating measurement failed?", messageType.equals(Message.CATEGORY.INFO));
    }
}
