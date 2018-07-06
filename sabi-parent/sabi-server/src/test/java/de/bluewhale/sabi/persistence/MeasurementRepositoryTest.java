/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
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
// @DataJpaTest todo does not work yet missing visible constructor in JPAConfig class - mayby not compatible with eclipse way?
public class MeasurementRepositoryTest {

    static TestDataFactory testDataFactory;

    @Autowired
    MeasurementRepository measurementRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UserRepository userRepository;

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
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(1L);
        AquariumEntity aquariumEntity = aquariumRepository.getOne(measurementTo.getAquariumId());

        MeasurementEntity measurementEntity = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        measurementEntity.setAquarium(aquariumEntity);

        // when
        MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);

        // then
        Assert.assertNotNull("Measurement was not persisted!", createdMeasurementEntity.getId());
        MeasurementEntity foundMeasurementEntity = measurementRepository.findById(createdMeasurementEntity.getId()).get();

        Assert.assertEquals(createdMeasurementEntity.getAquarium(), foundMeasurementEntity.getAquarium());
        Assert.assertEquals(createdMeasurementEntity.getAquarium().getId(), measurementTo.getAquariumId());

    }

    @Test
    @Transactional
    public void testFetchStoredTestUsersMeasurements() throws Exception {

        // given some stored testdata for userID 1
        UserEntity userEntity = userRepository.getOne(1L);

        // when
        List<MeasurementEntity> usersMeasurements = measurementRepository.findMeasurementEntitiesByUser(userEntity);

        // then
        Assert.assertTrue("We have two rows of stored testdata!?", usersMeasurements.size() == 2);

    }

    @Test
    public void testGetConcreteMeasurement() throws Exception {
        // Given some prestored testdata for userID 1
        Long testAquariumId = 1L;
        Long testUserId = 1L;
        UserEntity userEntity = userRepository.getOne(testUserId);

        // When
        MeasurementEntity measurement = measurementRepository.getMeasurementEntityByIdAndUser(100L, userEntity);

        // Then
        Assert.assertEquals("Missing testdata for user 1L",  measurement.getAquarium().getId(),testAquariumId);
    }


}
