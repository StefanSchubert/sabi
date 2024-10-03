/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.MeasurementMapper;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestContainerVersions;
import de.bluewhale.sabi.util.TestDataFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;


/**
 * Persistence-Layer Test for MeasurementDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("IntegrationTest")
@Transactional
@DirtiesContext
// DirtiesContext: Spring context is refreshed after the test class is executed,
// which includes reinitializing the HikariCP datasource (which is defined at spring level, while the testcontainer is not)
public class MeasurementRepositoryTest implements TestContainerVersions {

    static TestDataFactory testDataFactory;

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private Flyway flyway;

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

    @AfterAll
    static void cleanup() {
        mariaDBContainer.stop();
    }

    @BeforeEach
    public void setUp() {

        // flyway.clean(); // Optional: Clean DB before each single test
        // org.flywaydb.core.api.FlywayException: Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.
        flyway.migrate();

    }

    @Test
    void connectionEstablished(){
        assertThat(mariaDBContainer.isCreated());
        assertThat(mariaDBContainer.isRunning());
    }

    @Test
    @Rollback
    public void testCreateMeasurement() throws Exception {

        // given a test measurement (linked aquarium already exists in database.
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(null);
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
        assertEquals(createdMeasurementEntity.getAquarium().getId(), aquariumEntity.getId());

    }

    @Test
    public void testFetchStoredTestUsersMeasurements() throws Exception {

        // given some stored testdata for prestored user (via flyway)
        UserEntity userEntity = userRepository.getByEmail("sabi@bluewhale.de");
        // when
        List<MeasurementEntity> usersMeasurements = measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity);
        // then
        assertTrue("Basic testdata missing!?", usersMeasurements.size() > 0);
    }

    @Test
    public void testFetchStoredTestUsersMeasurementsWithResultLimit() throws Exception {

        // given some stored testdata for userID 1 (has two test measurements)
        UserEntity userEntity = userRepository.getByEmail("sabi@bluewhale.de");
        // when
        Pageable page = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "measuredOn"));
        List<MeasurementEntity> usersMeasurements = measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity,page);
        // then expect 2 because of paging out of 3 rows available via BasicTestDataFactory
        assertTrue("Basic testdata missing!?", usersMeasurements.size() == 2);
    }


    @Test
    @Rollback
    public void testGetConcreteMeasurement() throws Exception {

        // Given some pre-stored testdata for userID 1
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(null);
        AquariumEntity aquariumEntity = aquariumRepository.getOne(measurementTo.getAquariumId());
        MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo);
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setUser(aquariumEntity.getUser());
        MeasurementEntity createdMeasurementEntity = measurementRepository.saveAndFlush(measurementEntity);
        UserEntity userEntity = aquariumEntity.getUser();

        // When
        MeasurementEntity measurement = measurementRepository.getByIdAndUser(createdMeasurementEntity.getId(), userEntity);
        // Then
        assertTrue("Broken finder?",  measurement.getAquarium().getId()==aquariumEntity.getId());
    }


}
