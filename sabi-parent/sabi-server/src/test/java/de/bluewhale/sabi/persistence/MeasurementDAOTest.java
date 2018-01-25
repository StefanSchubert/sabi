/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.MeasurementDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.util.Mapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Persistence-Layer Test for MeasurementDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class MeasurementDAOTest {

    static TestDataFactory testDataFactory;

    @Autowired
    MeasurementDao measurementDao;

    @Autowired
    AquariumDao aquariumDao;

    @BeforeClass
    public static void init() {
        testDataFactory = TestDataFactory.getInstance();
    }


/*    @AfterClass
    public static void tearDownClass() throws Exception {
    }
*/


    @Test
    @Transactional
    public void testCreateMeasurement() throws Exception {

        // given a test measurement (linked aquarium already exists in database.
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo();
        AquariumEntity aquariumEntity = aquariumDao.find(measurementTo.getAquariumId());
        MeasurementEntity measurementEntity = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        measurementEntity.setAquarium(aquariumEntity);

        // when
        MeasurementEntity createdMeasurementEntity = measurementDao.create(measurementEntity);

        // then
        MeasurementEntity foundMeasurementEntity = measurementDao.find(createdMeasurementEntity.getId());

        Assert.assertEquals(createdMeasurementEntity.getAquarium(), foundMeasurementEntity.getAquarium());
        Assert.assertEquals(createdMeasurementEntity.getAquarium().getId(), measurementTo.getAquariumId());

    }

    @Test
    @Transactional
    public void testFetchStoredTestUsersMeasurements() throws Exception {

        // given some stored testdata for userID 1

        // when
        List<MeasurementTo> usersMeasurements = measurementDao.findUsersMeasurements(1L);

        // then
        Assert.assertTrue("We have two rows of stored testdata!?", usersMeasurements.size() == 2);

    }

    @Test
    public void testGetConcreteMeasurement() throws Exception {
        // Given some prestored testdata for userID 1

        // When
        MeasurementTo measurementTo = measurementDao.getUsersMeasurement(100L, 1L);

        // Then
        Assert.assertEquals("Missing testdata for user 1L", 1L, measurementTo.getAquariumId(),1L);
    }


}
