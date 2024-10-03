/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Behavioral test of the IoT APIs
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("APITest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AquariumIoTControllerTest {

    final static String MOCKED_USER = "testsabi@bluewhale.de";
    private static final String SECRET_ASSUMED_AS_VALID = "allowed api key";
    private static final String SECRET_ASSUMED_AS_INVALID = "invalid api key";


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


    @Autowired
    ObjectMapper objectMapper;  // json mapper

    @Autowired
    private TestRestTemplate restTemplate;

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
        HttpHeaders headers = RestHelper.buildHttpHeader();
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("/api/aquarium_iot/temp_measurement", entity, String.class);

        // then we should get a 400 as result because of activated JSR303 Validation
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));

    }

    @Test
    public void testAddMeasurementWithInvalidAPIKey() throws Exception {

        // given some invalid Testdata (via mocking)

        IoTMeasurementTo ioTMeasurementTo = new IoTMeasurementTo();
        ioTMeasurementTo.setApiKey(SECRET_ASSUMED_AS_INVALID);
        ioTMeasurementTo.setMeasuredValueInCelsius(29f);

        // when submitting an IoT Measurement
        String requestJson = objectMapper.writeValueAsString(ioTMeasurementTo);
        HttpHeaders headers = RestHelper.buildHttpHeader();
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.postForEntity("/api/aquarium_iot/temp_measurement", entity, String.class);
        } catch (ResourceAccessException e) {
            System.out.println(e);
            return;
            // That is acceptable and will be ignored.
            // I just don't want to add another HttpClient, just for the junit
            // see https://stackoverflow.com/questions/49119354/getting-java-net-httpretryexception-cannot-retry-due-to-server-authentication
        }

        // then we should get a 401 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));

    }

}