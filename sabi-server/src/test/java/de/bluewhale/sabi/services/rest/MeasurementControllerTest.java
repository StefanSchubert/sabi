/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.MeasurementEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.util.Mapper;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.Mapper.mapAquariumTo2Entity;
import static de.bluewhale.sabi.util.Mapper.mapMeasurementTo2EntityWithoutAquarium;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Demonstrate usage of the measurement REST API.
 * NOTICE: This test mocks the DAO persistent layer, as it was not meant to run as an integration test.
 * <p>
 * However notice the following drawbacks:
 * <p>
 * (1) It still requires the database, as without it we get a java.lang.IllegalStateException:
 * Failed to load ApplicationContext, though this might be fixed by proper test configuration
 * (2) Lines of code! The mocked variant outweighs the implementation by far. Which slows down development progress.
 * I leave it to demonstrate the effect. For those cases it would be much better to leave this as real integration
 * tests (however against an H2 in memory database, or by manually control your test data).
 *
 * @author Stefan Schubert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MeasurementControllerTest {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

    @MockBean
    UserRepository userRepository;

    @MockBean
    AquariumRepository aquariumRepository;

    @MockBean
    MeasurementRepository measurementRepository;
    @Autowired
    ObjectMapper objectMapper;  // json mapper
    TestDataFactory testDataFactory = TestDataFactory.getInstance();
    @Autowired
    private TokenAuthenticationService encryptionService;
    @Autowired
    private TestRestTemplate restTemplate;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListUsersMeasurements() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);


        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = new AquariumEntity();
        Mapper.mapAquariumTo2Entity(aquariumTo, aquariumEntity);
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());

        MeasurementEntity measurementEntity = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        measurementEntity.setAquarium(aquariumEntity);

        List<MeasurementEntity> testMeasurements = new ArrayList<>(1);
        testMeasurements.add(measurementEntity);

        given(this.measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity)).willReturn(testMeasurements);
        // given(this.measurementDao.findUsersMeasurements(userTo.getId())).willReturn(testMeasurements);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement/list/0", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test measurement
        MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
        assertThat(Arrays.asList(myObjects), hasItem(measurementTo));
    }

    @Test
    public void testListUsersTankMeasurements() throws Exception {
        // Given
        Long usersTankID = 1l;

        // and some mocked data
        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = new AquariumEntity();
        Mapper.mapAquariumTo2Entity(aquariumTo, aquariumEntity);
        aquariumEntity.setUser(userEntity);
        given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
        given(this.aquariumRepository.getOne(aquariumTo.getId())).willReturn(aquariumEntity);

        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());
        MeasurementTo measurementTo2 = testDataFactory.getTestMeasurementTo(aquariumTo.getId());
        MeasurementEntity measurementEntity = new MeasurementEntity();
        MeasurementEntity measurementEntity2 = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo2, measurementEntity2);
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity2.setAquarium(aquariumEntity);

        List<MeasurementEntity> testMeasurements = new ArrayList<>(2);
        testMeasurements.add(measurementEntity);
        testMeasurements.add(measurementEntity2);
        given(measurementRepository.findByAquarium(aquariumEntity)).willReturn(testMeasurements);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // When this authorized user, requests all measurements for a specific tank

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement/tank/" + usersTankID, HttpMethod.GET, requestEntity, String.class);

        // Then
        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
        // and our two prestored test measurements
        MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
        assertThat("Prestored data changed?", myObjects.length == 2);

    }

    @Test
    public void testListUsersTankMeasurementsForSpecificMeasurement() throws Exception {
        // Given
        Long usersTankID = 1l;
        Integer requestedUnitID = 1;

        // and some mocked data
        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = new AquariumEntity();
        Mapper.mapAquariumTo2Entity(aquariumTo, aquariumEntity);
        aquariumEntity.setUser(userEntity);
        given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
        given(this.aquariumRepository.getOne(aquariumTo.getId())).willReturn(aquariumEntity);

        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());
        MeasurementEntity measurementEntity = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        measurementEntity.setAquarium(aquariumEntity);

        List<MeasurementEntity> testMeasurements = new ArrayList<>(2);
        testMeasurements.add(measurementEntity);
        given(measurementRepository.findByAquariumAndUnitId(aquariumEntity, measurementTo.getUnitId())).willReturn(testMeasurements);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // When this authorized user, requests all measurements for a specific tank

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        String apiURL = String.format("/api/measurement/tank/%s/unit/%s", usersTankID, requestedUnitID);
        ResponseEntity<String> responseEntity = restTemplate.exchange(apiURL, HttpMethod.GET, requestEntity, String.class);

        // Then
        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
        // and our two prestored test measurements
        MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
        assertThat("Prestored data changed?", myObjects.length == 1);

    }


    @Test
    public void testTryToDeleteOtherUsersMeasurement() throws Exception {
        // Given some measurement we are trying to access
        Long measurementID = 856L;

        // and a currently authenticated user
        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // When this authorized user, tries to delete a measurement he does not own.
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement/" + measurementID, HttpMethod.DELETE, requestEntity, String.class);

        // Then
        assertThat("Unallowed access should produce a conflict", responseEntity.getStatusCode().equals(HttpStatus.CONFLICT));
    }


    @Test
    public void testAddMeasurement() throws Exception {

        // Given
        // a currently authenticated user
        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = new AquariumEntity();

        mapAquariumTo2Entity(aquariumTo, aquariumEntity);
        given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
        given(this.aquariumRepository.findById(aquariumTo.getId())).willReturn(Optional.of(aquariumEntity));

        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());
        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setId(88L);
        measurementEntity.setAquarium(aquariumEntity);
        mapMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
        given(this.measurementRepository.saveAndFlush(any())).willReturn(measurementEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // When this authorized user, tries to add a measurement
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        String requestJson = objectMapper.writeValueAsString(measurementTo);
        HttpEntity<String> requestEntity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement", HttpMethod.POST, requestEntity, String.class);

        // then we should get a 201 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // and our test measurement
        MeasurementTo createdMeasurement = objectMapper.readValue(responseEntity.getBody(), MeasurementTo.class);
        assertEquals(createdMeasurement.getId(), measurementEntity.getId());

    }

    @Test
    public void testUpdateMeasurement() throws Exception {
        // Given
        // a currently authenticated user
        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);

        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo, userEntity);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = new AquariumEntity();
        Mapper.mapAquariumTo2Entity(aquariumTo, aquariumEntity);
        MeasurementTo updatableMeasurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());
        updatableMeasurementTo.setId(88L);

        MeasurementEntity updatableMeasurementEntity = new MeasurementEntity();
        Mapper.mapMeasurementTo2EntityWithoutAquarium(updatableMeasurementTo, updatableMeasurementEntity);
        updatableMeasurementEntity.setId(updatableMeasurementTo.getId());
        updatableMeasurementEntity.setAquarium(aquariumEntity);

        MeasurementTo updatedMeasurementTo = new MeasurementTo();
        Mapper.mapMeasurementEntity2To(updatableMeasurementEntity, updatedMeasurementTo);
        updatedMeasurementTo.setMeasuredValue(updatableMeasurementTo.getMeasuredValue() + 2f);

        MeasurementEntity updatedMeasurementEntity = new MeasurementEntity();
        updatedMeasurementEntity.setAquarium(aquariumEntity);
        Mapper.mapMeasurementTo2EntityWithoutAquarium(updatableMeasurementTo, updatedMeasurementEntity);

        Optional<MeasurementEntity> optionalMeasurementEntity = Optional.of(updatableMeasurementEntity);

        given(this.measurementRepository.getByIdAndUser(updatableMeasurementTo.getId(), userEntity)).willReturn(updatableMeasurementEntity);
        given(this.measurementRepository.findById(updatableMeasurementTo.getId())).willReturn(optionalMeasurementEntity);
        given(this.measurementRepository.save(updatableMeasurementEntity)).willReturn(updatedMeasurementEntity);

        // When this authorized user, tries to update a measurement
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        String requestJson = objectMapper.writeValueAsString(updatableMeasurementTo);
        HttpEntity<String> requestEntity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement", HttpMethod.PUT, requestEntity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // and our test measurement
        MeasurementTo returnedMeasurement = objectMapper.readValue(responseEntity.getBody(), MeasurementTo.class);
        assertThat("Value not updated?", returnedMeasurement.getMeasuredValue() == updatedMeasurementEntity.getMeasuredValue());
    }


}
