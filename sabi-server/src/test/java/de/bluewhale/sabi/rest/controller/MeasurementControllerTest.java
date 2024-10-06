/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.AquariumMapper;
import de.bluewhale.sabi.mapper.MeasurementMapper;
import de.bluewhale.sabi.mapper.UserMapper;
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
import de.bluewhale.sabi.util.RestHelper;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Demonstrate usage of the measurement REST API.
 * NOTICE: This test mocks the DAO persistent layer, as it was not meant to run as an integration test.
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MeasurementControllerTest {
// ------------------------------ FIELDS ------------------------------

	final static String MOCKED_USER = "testsabi@bluewhale.de";

           /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

	@Container
	@ServiceConnection
	// This does the trick. Spring Autoconfigures itself to connect against this container data requests-
	static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

	@LocalServerPort
	private int port;

	private RestClient restClient;

	@BeforeEach
	public void initRestClient() {
		if (restClient == null) {
			String url = String.format("http://localhost:%d/sabi", port);
			restClient = RestClient
					.builder()
					.baseUrl(url) // Dynamischer Port
					.build();
		}
	}


	@MockBean
	UserRepository userRepository;

	@MockBean
	AquariumRepository aquariumRepository;

	@Autowired
	AquariumMapper aquariumMapper;

	@Autowired
	MeasurementMapper measurementMapper;

	@Autowired
	UserMapper userMapper;

	@MockBean
	MeasurementRepository measurementRepository;
	@Autowired
	ObjectMapper objectMapper;  // json mapper
	TestDataFactory testDataFactory = TestDataFactory.getInstance();
	@Autowired
	private TokenAuthenticationService encryptionService;


// -------------------------- OTHER METHODS --------------------------

	@Test
	public void testListUsersMeasurements() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo);

		MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo);
		measurementEntity.setAquarium(aquariumEntity);

		List<MeasurementEntity> testMeasurements = new ArrayList<>(1);
		testMeasurements.add(measurementEntity);

		given(this.measurementRepository.findByUserOrderByMeasuredOnDesc(userEntity)).willReturn(testMeasurements);
		// given(this.measurementDao.findUsersMeasurements(userTo.getId())).willReturn(testMeasurements);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests his aquarium list
		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the rest-template will get it as array.
		ResponseEntity<String> stringResponseEntity = restClient.get().uri("/api/measurement/list/0")
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// and our test measurement
		MeasurementTo[] myObjects = objectMapper.readValue(stringResponseEntity.getBody(), MeasurementTo[].class);
		assertThat(Arrays.asList(myObjects), hasItem(measurementTo));
	}

	@Test
	public void testListUsersTankMeasurements() throws Exception {
		// Given
		Long usersTankID = 1l;

		// and some mocked data
		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		aquariumEntity.setUser(userEntity);
		given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
		given(this.aquariumRepository.getOne(aquariumTo.getId())).willReturn(aquariumEntity);

		MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo);
		MeasurementTo measurementTo2 = testDataFactory.getTestMeasurementTo(aquariumTo);
		MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo);
		MeasurementEntity measurementEntity2 = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo2);
		measurementEntity.setAquarium(aquariumEntity);
		measurementEntity2.setAquarium(aquariumEntity);

		List<MeasurementEntity> testMeasurements = new ArrayList<>(2);
		testMeasurements.add(measurementEntity);
		testMeasurements.add(measurementEntity2);
		given(measurementRepository.findByAquarium(aquariumEntity)).willReturn(testMeasurements);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// When this authorized user, requests all measurements for a specific tank

		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the rest-template will get it as array.
		ResponseEntity<String> stringResponseEntity = restClient.get().uri("/api/measurement/tank/" + usersTankID)
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// Then we should get our two prestored test measurements
		MeasurementTo[] myObjects = objectMapper.readValue(stringResponseEntity.getBody(), MeasurementTo[].class);
		assertThat("Prestored data changed?", myObjects.length == 2);

	}

	@Test
	public void testListUsersTankMeasurementsForSpecificMeasurement() throws Exception {
		// Given
		Long usersTankID = 1l;
		Integer requestedUnitID = 1;

		// and some mocked data
		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		aquariumEntity.setUser(userEntity);
		given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
		given(this.aquariumRepository.getOne(aquariumTo.getId())).willReturn(aquariumEntity);

		MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo);
		MeasurementEntity measurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(measurementTo);
		measurementEntity.setAquarium(aquariumEntity);

		List<MeasurementEntity> testMeasurements = new ArrayList<>(2);
		testMeasurements.add(measurementEntity);
		given(measurementRepository.findByAquariumAndUnitIdOrderByMeasuredOnAsc(aquariumEntity, measurementTo.getUnitId())).willReturn(testMeasurements);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// When this authorized user, requests all measurements for a specific tank

		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the rest-template will get it as array.
		String apiURL = String.format("/api/measurement/tank/%s/unit/%s", usersTankID, requestedUnitID);
		ResponseEntity<String> stringResponseEntity = restClient.get().uri(apiURL)
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// Then we should get our two prestored test measurements
		MeasurementTo[] myObjects = objectMapper.readValue(stringResponseEntity.getBody(), MeasurementTo[].class);
		assertThat("Prestored data changed?", myObjects.length == 1);

	}


	@Test
	public void testTryToDeleteOtherUsersMeasurement() throws Exception {
		// Given some measurement we are trying to access
		Long measurementID = 856L;

		// and a currently authenticated user
		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// When this authorized user, tries to delete a measurement he does not own.
		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the rest-template will get it as array.
		try {
			ResponseEntity<String> stringResponseEntity = restClient.delete().uri("/api/measurement/" + measurementID)
					.headers(headers -> headers.addAll(authedHeader))
					.retrieve()
					.toEntity(String.class);

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Conflict to be thrown");

		} catch (HttpClientErrorException e) {
			// then we should get a 409 as result because of invalid user
			assertThat("Unallowed access should produce a conflict", e.getStatusCode().equals(HttpStatus.CONFLICT));
		}

	}


	@Test
	public void testAddMeasurement() throws Exception {

		// Given
		// a currently authenticated user
		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);

		given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);
		given(this.aquariumRepository.findById(aquariumTo.getId())).willReturn(Optional.of(aquariumEntity));

		MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo);
		MeasurementEntity measurementEntity = new MeasurementEntity();
		measurementEntity.setId(88L);
		measurementEntity.setAquarium(aquariumEntity);
		measurementMapper.mergeMeasurementTo2EntityWithoutAquarium(measurementTo, measurementEntity);
		given(this.measurementRepository.saveAndFlush(any())).willReturn(measurementEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// When this authorized user, tries to add a measurement
		HttpHeaders authedHttpHeader = RestHelper.prepareAuthedHttpHeader(authToken);
		String requestJson = objectMapper.writeValueAsString(measurementTo);

		ResponseEntity<String> responseEntity = restClient.post()
				.uri("/api/measurement")
				.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 201, (request, response) -> {
					// then we should get a 201 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity

		// and we should get our test measurement
		MeasurementTo createdMeasurement = objectMapper.readValue(responseEntity.getBody(), MeasurementTo.class);
		assertEquals(createdMeasurement.getId(), measurementEntity.getId());

	}

	@Test
	public void testUpdateMeasurement() throws Exception {
		// Given
		// a currently authenticated user
		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		MeasurementTo updatableMeasurementTo = testDataFactory.getTestMeasurementTo(aquariumTo);
		updatableMeasurementTo.setId(88L);

		MeasurementEntity updatableMeasurementEntity = measurementMapper.mapMeasurementTo2EntityWithoutAquarium(updatableMeasurementTo);
		updatableMeasurementEntity.setId(updatableMeasurementTo.getId());
		updatableMeasurementEntity.setAquarium(aquariumEntity);

		MeasurementTo updatedMeasurementTo = measurementMapper.mapMeasurementEntity2To(updatableMeasurementEntity);
		updatedMeasurementTo.setMeasuredValue(updatableMeasurementTo.getMeasuredValue() + 2f);

		MeasurementEntity updatedMeasurementEntity = new MeasurementEntity();
		updatedMeasurementEntity.setAquarium(aquariumEntity);
		measurementMapper.mergeMeasurementTo2EntityWithoutAquarium(updatableMeasurementTo, updatedMeasurementEntity);

		Optional<MeasurementEntity> optionalMeasurementEntity = Optional.of(updatableMeasurementEntity);

		given(this.measurementRepository.getByIdAndUser(updatableMeasurementTo.getId(), userEntity)).willReturn(updatableMeasurementEntity);
		given(this.measurementRepository.findById(updatableMeasurementTo.getId())).willReturn(optionalMeasurementEntity);
		given(this.measurementRepository.save(updatableMeasurementEntity)).willReturn(updatedMeasurementEntity);

		// When this authorized user, tries to update a measurement
		HttpHeaders authedHttpHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		String requestJson = objectMapper.writeValueAsString(updatableMeasurementTo);

		ResponseEntity<String> responseEntity = restClient.put()
				.uri("/api/measurement")
				.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 200, (request, response) -> {
					// then we should get a 200 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity

		// and we should get our test measurement
		MeasurementTo returnedMeasurement = objectMapper.readValue(responseEntity.getBody(), MeasurementTo.class);
		assertThat("Value not updated?", returnedMeasurement.getMeasuredValue() == updatedMeasurementEntity.getMeasuredValue());
	}

}
