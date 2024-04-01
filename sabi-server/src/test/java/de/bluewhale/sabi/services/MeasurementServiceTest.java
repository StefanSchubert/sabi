/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static de.bluewhale.sabi.TestDataFactory.TEST_TANK_ID;
import static de.bluewhale.sabi.configs.TestContainerVersions.MARIADB_11_3_2;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.util.AssertionErrors.*;


/**
 * Business-Layer tests for MeasurementServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("ServiceTest")
@DirtiesContext
public class MeasurementServiceTest {

    /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

    // ------------------------------ FIELDS ------------------------------

    static TestDataFactory testDataFactory;

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private MeasurementService measurementService;

    @MockBean
    private AquariumRepository aquariumRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UnitRepository unitRepository;

    @MockBean
    private MeasurementRepository measurementRepository;

    @MockBean
    private LocalizedUnitRepository localizedUnitRepository;

    @MockBean
    ParameterRepository parameterRepository;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListMeasurements() throws Exception {

        // Given:
        TestDataFactory testDataFactory = TestDataFactory.getInstance();

        UserTo testUserTo = TestDataFactory.getInstance().getNewTestUserTo(TestDataFactory.TESTUSER_EMAIL1);
        UserEntity userEntity = TestDataFactory.getInstance().getNewTestUserEntity(testUserTo);

        AquariumTo testAquariumTo = testDataFactory.getTestAquariumTo();
        AquariumEntity aquariumEntity = testDataFactory.getTestAquariumEntity(testAquariumTo, userEntity);

        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(testAquariumTo);
        MeasurementEntity measurementEntity = testDataFactory.getTestMeasurementEntity(testMeasurementTo, aquariumEntity);


        given(aquariumRepository.getOne(TEST_TANK_ID)).willReturn(aquariumEntity);
        given(measurementRepository.findByAquarium(aquariumEntity)).willReturn(List.of(measurementEntity));
        given(userRepository.getByEmail(TestDataFactory.TESTUSER_EMAIL1)).willReturn(userEntity);
        given(measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity)).willReturn(List.of(measurementEntity));

        // When
        List<MeasurementTo> tank1Measurements = measurementService.listMeasurements(TEST_TANK_ID);
        List<MeasurementTo> usersMeasurements = measurementService.listMeasurements(TestDataFactory.TESTUSER_EMAIL1, 0);

        // Then
        assertNotNull("Should not happen!", tank1Measurements);
        assertNotNull("Should not happen!", usersMeasurements);
        assertTrue("Mocks didn't worked?", tank1Measurements.size() >= 1);
        assertTrue("Relationships brocken?", usersMeasurements.containsAll(tank1Measurements));
    }


    @Test
    public void testFindLocalizedMeasurementParameter() throws Exception {
        // Given parameter Entity with id 1
        LocalizedParameterEntity localizedParameterEntity = new LocalizedParameterEntity();
        localizedParameterEntity.setId(1L);
        localizedParameterEntity.setLanguage("de");
        localizedParameterEntity.setDescription("Test Parameter");

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setId(1);
        parameterEntity.setLocalizedParameterEntities(List.of(localizedParameterEntity));

        given(parameterRepository.findByBelongingUnitIdEquals(1)).willReturn(parameterEntity);

        // When
        ParameterTo parameterTo = measurementService.fetchParameterInfoFor(1, "de");

        // Then
        assertNotNull("Should not happen!", parameterTo);
    }

    @Test
    public void testFindInvalidMeasurementParameter() throws Exception {
        // Given
        Integer nonExistingUnit = Integer.MAX_VALUE;
        given(parameterRepository.findByBelongingUnitIdEquals(nonExistingUnit)).willReturn(null);

        // When
        ParameterTo parameterTo = measurementService.fetchParameterInfoFor(nonExistingUnit, "de");

        // Then
        assertNull("According to API we should get null but got: ", parameterTo);
    }

    @Test
    public void testListMeasurementsForSpecificTankAndUnit() throws Exception {
        // Given
        AquariumEntity aquariumEntity = new AquariumEntity();
        aquariumEntity.setId(2L);

        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setUnitId(1);

        given(aquariumRepository.getOne(2L)).willReturn(aquariumEntity);   // Tank
        given(measurementRepository.findByAquariumAndUnitIdOrderByMeasuredOnAsc(aquariumEntity, 1)).willReturn(List.of(measurementEntity));

        // When
        List<MeasurementTo> measurements = measurementService.listMeasurementsFilteredBy(2L, 1);

        // Then
        assertNotNull("Should not happen!", measurements);
        assertTrue("Mocked Testdata gone or changed? Received more or less than expected on measurement.", measurements.size() == 1);
    }

    @Test
    public void testListMeasurementUnits() throws Exception {
        // Given already stored testdata for measurements
        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setName("Happyness");
        unitEntity.setId(1);
        LocalizedUnitEntity localizedUnitEntity = new LocalizedUnitEntity();
        localizedUnitEntity.setLanguage("de");
        localizedUnitEntity.setDescription("Glück");

        given(unitRepository.findAll()).willReturn(List.of(unitEntity));
        given(localizedUnitRepository.findByLanguageAndUnitId("de",unitEntity.getId())).willReturn(localizedUnitEntity);

        // When
        List<UnitTo> measurementUnits = measurementService.listAllMeasurementUnits("de");

        // Then
        assertNotNull("Should not happen!", measurementUnits);
        assertTrue("Testdata gone?", measurementUnits.size() >= 1);
        assertTrue("Translation failed?", measurementUnits.get(0).getDescription().equals("Glück"));
    }

    @Test
    @Rollback
    public void testAddNewMeasurement() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUserTo);
        AquariumTo testAquariumTo = testDataFactory.getTestAquariumTo();
        AquariumEntity aquariumEntity = testDataFactory.getTestAquariumEntity(testAquariumTo, userEntity);
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(testAquariumTo);

        given(userRepository.getByEmail(TestDataFactory.TESTUSER_EMAIL1)).willReturn(userEntity);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(TEST_TANK_ID,userEntity.getId())).willReturn(aquariumEntity);
        given(measurementRepository.saveAndFlush(any())).willReturn(new MeasurementEntity());

        // When adding a new Measurement
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addMeasurement(testMeasurementTo, TestDataFactory.TESTUSER_EMAIL1);

        // Then
        assertNotNull("Should not happen!",measurementToResultTo);
        assertNotNull("Should not happen! Empty ResultTo",measurementToResultTo.getValue());
        assertEquals("Creating measurement failed? " + measurementToResultTo.getMessage().getCode(), Message.CATEGORY.INFO, measurementToResultTo.getMessage().getType());
    }

    @Test
    @Rollback
    public void testRemoveMeasurement() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance();

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(TestDataFactory.TESTUSER_EMAIL1);
        userEntity.setId(99L);

        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setId(42L);

        given(userRepository.getByEmail(TestDataFactory.TESTUSER_EMAIL1)).willReturn(userEntity);
        given(measurementRepository.getByIdAndUser(42L, userEntity)).willReturn(measurementEntity);
        doNothing().when(measurementRepository).delete(measurementEntity);

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.removeMeasurement(42L, userEntity.getEmail());

        // Then
        assertNotNull("Should not happen!",measurementToResultTo);
        assertNotNull("Should not happen! ResultTo contains no Value!",measurementToResultTo.getValue());
        Message.CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Removal of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }

    @Test
    @Rollback
    public void testUpdateMeasurement() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        Long tankID = 4711L;
        float oldValue = 1.0f;
        float newValue = 2.0f;

        UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUserTo);

        AquariumTo testAquariumTo = testDataFactory.getTestAquariumFor(testUserTo);

        MeasurementEntity measurementEntity = testDataFactory.getTestMeasurementEntityWithDefaults();
        measurementEntity.setMeasuredValue(oldValue);
        measurementEntity.setId(42L);

        MeasurementEntity updatedMeasurementEntity = testDataFactory.getTestMeasurementEntityWithDefaults();
        updatedMeasurementEntity.setMeasuredValue(newValue);
        updatedMeasurementEntity.setId(42L);

        given(userRepository.getByEmail(TestDataFactory.TESTUSER_EMAIL1)).willReturn(userEntity);
        given(measurementRepository.getByIdAndUser(42L, userEntity)).willReturn(measurementEntity);
        given(measurementRepository.save(any())).willReturn(updatedMeasurementEntity);

        // When we update the measurement
        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(testAquariumTo);
        testMeasurementTo.setMeasuredValue(newValue);
        testMeasurementTo.setId(measurementEntity.getId());

        ResultTo<MeasurementTo> measurementToResultTo = measurementService.updateMeasurement(testMeasurementTo, TestDataFactory.TESTUSER_EMAIL1);

        // Then
        assertNotNull("Should not happen!",measurementToResultTo);
        assertEquals("Update failed?",newValue, measurementToResultTo.getValue().getMeasuredValue());
        Message.CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Update of measurement failed?", messageType.equals(Message.CATEGORY.INFO));

    }


    @Test
    @Rollback
    public void testAddIotAuthorizedMeasurement() {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance();

        UserTo testUserTo = testDataFactory.getNewTestUserTo(testDataFactory.TESTUSER_EMAIL1);
        UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUserTo);

        AquariumTo testAquariumTo = testDataFactory.getTestAquariumFor(testUserTo);
        AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(testAquariumTo, userEntity);

        MeasurementTo testMeasurementTo = testDataFactory.getTestMeasurementTo(testAquariumTo);
        MeasurementEntity createdMeasurementEntity = testDataFactory.getTestMeasurementEntity(testMeasurementTo, testAquariumEntity);

        given(aquariumRepository.findById(TEST_TANK_ID)).willReturn(Optional.of(testAquariumEntity));
        given(measurementRepository.saveAndFlush(any())).willReturn(createdMeasurementEntity);

        // When
        ResultTo<MeasurementTo> measurementToResultTo = measurementService.addIotAuthorizedMeasurement(testMeasurementTo);

        // Then
        assertNotNull("Should not happen!",measurementToResultTo);
        assertNotNull("Should not happen! ResultTO contains no value",measurementToResultTo.getValue());
        Message.CATEGORY messageType = measurementToResultTo.getMessage().getType();
        assertTrue("Creating measurement failed?", messageType.equals(Message.CATEGORY.INFO));
    }

}
