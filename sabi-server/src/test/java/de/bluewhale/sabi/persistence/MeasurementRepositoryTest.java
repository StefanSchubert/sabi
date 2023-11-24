/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.BasicDataFactory;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.MeasurementMapper;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;


/**
 * Persistence-Layer Test for MeasurementDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// @DataJpaTest todo does not work yet missing visible constructor in JPAConfig class - maybe not compatible with eclipse way?
public class MeasurementRepositoryTest extends BasicDataFactory {

    static TestDataFactory testDataFactory;

    @Autowired
    MeasurementRepository measurementRepository;

    @Autowired
    MeasurementMapper measurementMapper;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeAll
    public static void initTestDataFactory() {
        testDataFactory = TestDataFactory.getInstance();
    }

    /**
     * There seems to be a timing problem with H2, that causes, that the basic data is not available
     * for some test classes, while for others it worked out. Until we know what's going wrong...
     * we "double inject" by extending the BasicTestDataFactory and by calling it directly.
     * The different behaviour can be observed by e.g. calling the master test suite and as comparising
     * the measurement testsuite while this is method is deaktivated.
     */
    @BeforeEach
    public void ensureBasicDataAvailability() {

        UserEntity byEmail = userRepository.getByEmail(P_USER1_EMAIL);
        if (byEmail == null) populateBasicData();
        UserEntity byEmail2 = userRepository.getByEmail(P_USER1_EMAIL);
        assertNotNull("H2-Basicdata injection did not work!" ,byEmail2);
    }

    @Test
    @Transactional
    public void testCreateMeasurement() throws Exception {

        // given a test measurement (linked aquarium already exists in database.
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(1L);
        AquariumEntity aquariumEntity = aquariumRepository.getOne(measurementTo.getAquariumId());

        MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo);
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setUser(aquariumEntity.getUser());

        // when
        MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);

        // then
        assertNotNull("Measurement was not persisted!", createdMeasurementEntity.getId());
        MeasurementEntity foundMeasurementEntity = measurementRepository.findById(createdMeasurementEntity.getId()).get();

        assertEquals(createdMeasurementEntity.getAquarium(), foundMeasurementEntity.getAquarium());
        assertEquals(createdMeasurementEntity.getAquarium().getId(), measurementTo.getAquariumId());

    }

    @Test
    public void testFetchStoredTestUsersMeasurements() throws Exception {

        // given some stored testdata for userID 1
        UserEntity userEntity = userRepository.getOne(1L);
        // when
        List<MeasurementEntity> usersMeasurements = measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity);
        // then
        assertTrue("Basic testdata missing!?", usersMeasurements.size() > 0);
    }

    @Test
    public void testFetchStoredTestUsersMeasurementsWithResultLimit() throws Exception {

        // given some stored testdata for userID 1 (has two test measurements)
        UserEntity userEntity = userRepository.getOne(1L);
        // when
        Pageable page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "measuredOn"));
        List<MeasurementEntity> usersMeasurements = measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity,page);
        // then expect 2 because of paging out of 3 rows available via BasicTestDataFactory
        assertTrue("Basic testdata missing!?", usersMeasurements.size() == 2);
    }


    @Test
    public void testGetConcreteMeasurement() throws Exception {
        // Given some prestored testdata for userID 1
        Long testAquariumId = 1L;
        Long testUserId = 1L;
        Long testMeasurementId = 1L;
        UserEntity userEntity = userRepository.getOne(testUserId);
        // When
        MeasurementEntity measurement = measurementRepository.getByIdAndUser(testMeasurementId, userEntity);
        // Then
        assertTrue("Missing testdata for user 1L",  measurement.getAquarium().getId()==testAquariumId);
    }


}
