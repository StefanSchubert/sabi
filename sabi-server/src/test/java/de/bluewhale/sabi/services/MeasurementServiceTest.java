/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
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
     * for some test classes, while for others it worked out. Until we know what's going wrong...
     * we "double inject" by extending the BasicTestDataFactory and by calling it directly.
     * The different behaviour can be observed by e.g. calling the master test suite and as comparising
     * the measurement testsuite while this is method is deaktivated.
     */
    @Before
    public void ensureBasicDataAvailability() {
        List<MeasurementTo> list = measurementService.listMeasurements(P_USER1_EMAIL, 0);
        if (list.isEmpty()) populateBasicData();
        List<MeasurementTo> list2 = measurementService.listMeasurements(P_USER1_EMAIL, 0);
        assertTrue("H2-Basicdata injection did not work!" ,list2.size()>0);
    }

    @Test
    @Transactional
    public void testListMeasurements() throws Exception {
        // Given already stored testdata for measurements

        // When
        List<MeasurementTo> tank1Measurements = measurementService.listMeasurements(1L);
        List<MeasurementTo> usersMeasurements = measurementService.listMeasurements(P_USER1_EMAIL, 0);

        // Then
        assertNotNull(tank1Measurements);
        assertNotNull(usersMeasurements);
        assertTrue("Testdata gone?", tank1Measurements.size() >= 1);
        assertTrue("Stored Testdata changed?", usersMeasurements.containsAll(tank1Measurements));
    }

    @Test
    @Transactional
    public void testListMeasurementsForSpecificTankAndUnit() throws Exception {
        // Given already stored testdata for measurements
        // for tank 2 only one measurement with unit id 1

        // When
        List<MeasurementTo> measurements = measurementService.listMeasurementsFilteredBy(2L,1);

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
        // Given already store test data (tank 1L for user 1L)
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(1L);

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addMeasurement(testMeasurementTo, P_USER1_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Creating measurement failed?", messageType.equals(Message.CATEGORY.INFO));
    }

    @Test
    @Transactional
    public void testRemoveMeasurement() throws Exception {
        // Given a stored measurement for a tank and user
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        testDataFactory.withUserService(userService);
        testDataFactory.withTankService(tankService);
        String newTestUserMail = "junit@sabi.de";
        UserTo persistedTestUserTo = testDataFactory.getPersistedTestUserTo(newTestUserMail);
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
        assertEquals(newValue, measurementToResultTo.getValue().getMeasuredValue(),0f);
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Update of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }


}
