/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.MeasurementRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Demonstrate usage of the unit REST API.
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
public class StatsControllerTest extends CommonTestController {
// ------------------------------ FIELDS ------------------------------

	final static String MOCKED_USER = "testsabi@bluewhale.de";


	@MockBean
	MeasurementRepository measurementRepository;

	@MockBean
	UserRepository userRepository;

	@Autowired
	UserMapper userMapper;

	@Autowired
	ObjectMapper objectMapper;  // json mapper

	@Autowired
	private TokenAuthenticationService encryptionService;

// -------------------------- OTHER METHODS --------------------------

	@Test
	public void testRequestMeasurementCount() throws Exception {

		// given some Testdata via mocking
		UserTo userTo = new UserTo(MOCKED_USER, "Mocker User", "pw123");
		userTo.setId(1L);

		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);
		given(this.measurementRepository.count()).willReturn(1L);

		// and we need a valid authentication token for our mocked user
		String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

		// when this authorized user requests the unit list
		HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

		// Notice the that the controller defines a list, the rest-template will get it as array.
		ResponseEntity<String> stringResponseEntity = restClient.get().uri(Endpoint.MEASUREMENT_STATS.getPath())
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 200, (request, response) -> {
					// then we should get a 200 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// and we should get our overall measurement count
		String measurementCount = objectMapper.readValue(stringResponseEntity.getBody(), String.class);
		assertThat(measurementCount, containsString("1"));
	}

}
