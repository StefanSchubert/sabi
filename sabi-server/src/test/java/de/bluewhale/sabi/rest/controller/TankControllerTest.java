/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.AquariumMapper;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
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
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;
import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Demonstrate usage of the tank API.
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TankControllerTest {
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
	ObjectMapper objectMapper;  // json mapper

	@Autowired
	AquariumMapper aquariumMapper;

	@Autowired
	UserMapper userMapper;

	TestDataFactory testDataFactory = TestDataFactory.getInstance();

// -------------------------- OTHER METHODS --------------------------

	@Test
	public void testListUsersTank() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);
		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		List<AquariumTo> testAquariums = new ArrayList<>(1);
		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		testAquariums.add(aquariumTo);

		List<AquariumEntity> testAquariumEntities = new ArrayList<>(1);
		mapAquariumTOs2AquariumEntities(testAquariums, testAquariumEntities, userEntity);

		given(this.aquariumRepository.findAllByUser_IdIs(userTo.getId())).willReturn(testAquariumEntities);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests his aquarium list
		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the restClient will get it as array.
		ResponseEntity<String> stringResponseEntity = restClient.get().uri("/api/tank/list")
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// and we should get our test aquarium
		AquariumTo[] myObjects = objectMapper.readValue(stringResponseEntity.getBody(), AquariumTo[].class);
		boolean contained = false;
		for (AquariumTo aquarium : myObjects) {
			if (aquarium.equals(aquariumTo)) {
				contained = true;
				break;
			}
		}
		assertTrue("Did not received mockd Aquarium", contained);

	}

	private void mapAquariumTOs2AquariumEntities(List<AquariumTo> testAquariums,
												 List<AquariumEntity> testAquariumEntities,
												 UserEntity pOwner) {
		for (AquariumTo testAquarium : testAquariums) {
			AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(testAquarium);
			aquariumEntity.setUser(pOwner);
			testAquariumEntities.add(aquariumEntity);
		}
	}

	@Test
	public void testGetUsersTank() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		aquariumEntity.setUser(userEntity); // ToMappper does not Map the User

		given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests his aquarium list
		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the restClient will get it as array.
		ResponseEntity<String> stringResponseEntity = restClient.get().uri("/api/tank/"+aquariumTo.getId())
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 200, (request, response) -> {
					// then we should get a 200 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// and we should get our test aquarium
		AquariumTo myObject = objectMapper.readValue(stringResponseEntity.getBody(), AquariumTo.class);
		assertEquals(myObject.getDescription(), aquariumTo.getDescription());
	}


	@Test
	public void testCreateUsersTank() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity storedUserEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(storedUserEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity createdAquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		createdAquariumEntity.setUser(storedUserEntity);

		given(this.aquariumRepository.saveAndFlush(any())).willReturn(createdAquariumEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests to create a aquarium
		HttpHeaders authedHttpHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		String requestJson = objectMapper.writeValueAsString(aquariumTo);

		ResponseEntity<String> responseEntity = restClient.post()
				.uri("/api/tank/create")
				.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 201, (request, response) -> {
					// then we should get a 201 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity

		// and we should get our test aquarium
		AquariumTo createdAquarium = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
		assertEquals(createdAquarium.getDescription(), aquariumTo.getDescription());
	}

	@Test
	public void testRemoveUsersTank() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity existingAquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
		existingAquariumEntity.setUser(userEntity);

		given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(existingAquariumEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests to create a aquarium
		HttpHeaders authedHttpHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		ResponseEntity<String> responseEntity = restClient.delete()
				.uri("/api/tank/" + aquariumTo.getId())
				.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 200, (request, response) -> {
					// then we should get a 200 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity

	}


	@Test
	public void testUpdateUsersTank() throws Exception {
		// given some Testdata via mocking

		UserTo userTo = new UserTo(MOCKED_USER, "MockerUser", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);
		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

		// This represents to TO/Entity bevore the update
		AquariumTo updatableAquariumTo = testDataFactory.getTestAquariumFor(userTo);
		AquariumEntity updatableAquariumEntity = aquariumMapper.mapAquariumTo2Entity(updatableAquariumTo);
		updatableAquariumEntity.setUser(userEntity);

		// Here we prepare the updated Entity which will be returned after the update
		AquariumEntity updatedAquariumEntity = aquariumMapper.mapAquariumTo2Entity(updatableAquariumTo);
		updatedAquariumEntity.setUser(userEntity);
		String updateTestString = "Updated";
		updatedAquariumEntity.setDescription(updateTestString); // we test only on description in this test

		// MockInit
		// return the Entity as it was before the update
		given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(updatableAquariumTo.getId(), userTo.getId())).willReturn(updatableAquariumEntity);
		given(aquariumRepository.getOne(updatableAquariumTo.getId())).willReturn(updatableAquariumEntity);
		// when saving return the prepared updated entity
		given(aquariumRepository.saveAndFlush(any())).willReturn(updatedAquariumEntity);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests to update an aquarium
		HttpHeaders authedHttpHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		updatableAquariumTo.setDescription(updateTestString);
		String requestJson = objectMapper.writeValueAsString(updatableAquariumTo);

		ResponseEntity<String> responseEntity = restClient.put()
				.uri("/api/tank")
				.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 200, (request, response) -> {
					// then we should get a 200 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity

		// and we should get our test aquarium
		AquariumTo updatedAquarium = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
		assertEquals(updatedAquarium.getDescription(), updatableAquariumTo.getDescription());
	}


	@Test
	/**
	 * Test to check that our WebSecurityConfig is effective.
	 */
	public void testUnauthorizedListUsersTankRequest() throws Exception {

		// Given User presentation by a faked auth token
		String authToken = "faked";

		// when this authorized user requests his aquarium list
		HttpHeaders authedHttpHeader = new HttpHeaders();
		// headers.setContentType(MediaType.APPLICATION_JSON);
		authedHttpHeader.add(AUTH_TOKEN, TOKEN_PREFIX + authToken);

		try {
			ResponseEntity<String> responseEntity = restClient.get()
					.uri("/api/tank/list")
					.headers(headers -> headers.addAll(authedHttpHeader))  // Set headers
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(String.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Unauthorized to be thrown");

		} catch (HttpClientErrorException e) {
			// then we should get a 401 as result because of invalid token
			assertThat("Faked access token should produce an unauthorized status.", e.getStatusCode().equals(HttpStatus.UNAUTHORIZED));

		}

	}

}
