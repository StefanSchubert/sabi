/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;


/**
 * Business-Layer tests for MeasurementServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MeasurementServiceTest {

    // pre existing test user
    private static final String P_USER_EMAIL = "sabi@bluewhale.de";
    // ------------------------------ FIELDS ------------------------------

    @Autowired
    private TankService tankService;

    @Autowired
    private UserService userService;

    @Autowired
    private MeasurementService measurementService;

// -------------------------- OTHER METHODS --------------------------

    @Test
    @Transactional
    public void testListMeasurements() throws Exception {
        // Given already stored testdata for measurements

        // When
        List<MeasurementTo> tank1Measurements = measurementService.listMeasurements(1L);
        List<MeasurementTo> usersMeasurements = measurementService.listMeasurements(P_USER_EMAIL);

        // Then
        assertNotNull(tank1Measurements);
        assertNotNull(usersMeasurements);
        assertTrue("Testdata gone?", tank1Measurements.size() > 1);
        assertTrue("Stored Testdata changed?", tank1Measurements.containsAll(usersMeasurements));
    }

    @Test
    @Transactional
    public void testAddNewMeasurement() throws Exception {
        // Given already store test data (tank 1L for user 1L)
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(1L);

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addMeasurement(testMeasurementTo, P_USER_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Creating measurement failed?", messageType.equals(Message.CATEGORY.INFO));
    }

    @Test
    @Transactional
    public void testRemoveMeasurement() throws Exception {
        // Given already stored measurement (ID 100L)

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.removeMeasurement(100L, P_USER_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertNotNull(measurementToResultTo.getValue());
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Removal of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }

    @Test
    @Transactional
    public void testUpdateMeasurement() throws Exception {
        // Given already stores measurments
        List<MeasurementTo> measurementToList = measurementService.listMeasurements(P_USER_EMAIL);
        MeasurementTo prestoresMeasurementTo = measurementToList.get(0);
        float oldValue = prestoresMeasurementTo.getMeasuredValue();

        // When we update the measurement
        float newValue = oldValue + 1.5f;
        prestoresMeasurementTo.setMeasuredValue(newValue);
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.updateMeasurement(prestoresMeasurementTo, P_USER_EMAIL);

        // Then
        assertNotNull(measurementToResultTo);
        assertEquals(newValue, measurementToResultTo.getValue().getMeasuredValue(),0f);
        CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Update of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }


}
