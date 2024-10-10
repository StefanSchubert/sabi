/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.IoTMeasurementTo;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

/**
 * Behavioral test of the IoT APIs
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AquariumIoTControllerTest extends CommonTestController {

	final static String MOCKED_USER = "testsabi@bluewhale.de";
	private static final String SECRET_ASSUMED_AS_VALID = "allowed api key";
	private static final String SECRET_ASSUMED_AS_INVALID = "invalid api key";

	@Autowired
	ObjectMapper objectMapper;  // json mapper

	@MockBean
	UserRepository userRepository;

	@MockBean
	TankService tankService;

	@Test
	public void testAddInvalidMeasurement() throws Exception {

		// given some invalid Testdata (via mocking)
		IoTMeasurementTo ioTMeasurementTo = new IoTMeasurementTo();
		ioTMeasurementTo.setApiKey(SECRET_ASSUMED_AS_VALID);
		ioTMeasurementTo.setMeasuredValueInCelsius(40f); // <-- Out of allowed range

		// when submitting an IoT Measurement
		String requestJson = objectMapper.writeValueAsString(ioTMeasurementTo);

		try {
			ResponseEntity<String> responseEntity = restClient.post()
					.uri(Endpoint.IOT_API.getPath() +"/temp_measurement")
					.headers(headers -> headers.addAll(RestHelper.buildHttpHeader()))  // Set headers
					.body(requestJson)  // Set the request body
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(String.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$BadRequest to be thrown");

		} catch (HttpClientErrorException e) {
			// then we should get a 400 as result because of activated JSR303 Validation
			assertThat(e.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
			// Optional: Check the error message or body if needed
			assertThat(e.getResponseBodyAsString(), containsString("muss kleiner-gleich 35 sein"));
		}

	}

	@Test
	public void testAddMeasurementWithInvalidAPIKey() throws Exception {

		// given some invalid Testdata (via mocking)

		IoTMeasurementTo ioTMeasurementTo = new IoTMeasurementTo();
		ioTMeasurementTo.setApiKey(SECRET_ASSUMED_AS_INVALID);
		ioTMeasurementTo.setMeasuredValueInCelsius(29f);

		// when submitting an IoT Measurement
		String requestJson = objectMapper.writeValueAsString(ioTMeasurementTo);

		try {
			ResponseEntity<String> responseEntity = restClient.post()
					.uri(Endpoint.IOT_API.getPath() +"/temp_measurement")
					.headers(headers -> headers.addAll(RestHelper.buildHttpHeader()))  // Set headers
					.body(requestJson)  // Set the request body
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(String.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Unauthorized to be thrown");

		} catch (HttpClientErrorException e) {
    		// then we should get a 401 as result.
			assertThat(e.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
		}

	}

}