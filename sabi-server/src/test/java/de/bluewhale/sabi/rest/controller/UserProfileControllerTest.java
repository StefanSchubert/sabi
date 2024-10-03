/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.CommonMessageCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.UserService;
import de.bluewhale.sabi.util.RestHelper;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Checks UserProfile Rest API
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("APITest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserProfileControllerTest {

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

    @MockBean
    UserService userService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

    TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @AfterEach
    public void cleanUpMocks() {
        reset(userService);
    }

    /**
     * Tests userprofile Rest API in successful update case.
     *
     * @throws Exception
     */
    @Test // REST-API
    public void testUnauthUserProfileUpdate() throws Exception {

        // given a successful update answer
        UserProfileTo userProfileTo = testDataFactory.getBasicUserProfileTo();
        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        given(this.userService.updateProfile(userProfileTo, "junit@maven.here")).willReturn(userProfileResultTo);

        // when - sending an update request
        HttpHeaders headers = RestHelper.buildHttpHeader();
        String requestJson = objectMapper.writeValueAsString(userProfileTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.USER_PROFILE.getPath(), HttpMethod.PUT, entity, String.class);

        // then we should get a 403 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.FORBIDDEN));
    }

    /**
     * Tests userprofile Rest API in successful update case.
     *
     * @throws Exception
     */
    @Test // REST-API
    public void testUserProfileUpdate() throws Exception {

        // given a successful update answer
        UserProfileTo userProfileTo = testDataFactory.getBasicUserProfileTo();
        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        given(this.userService.updateProfile(userProfileTo, TestDataFactory.TESTUSER_EMAIL1)).willReturn(userProfileResultTo);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending an update request
        String requestJson = objectMapper.writeValueAsString(userProfileTo);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.USER_PROFILE.getPath(), HttpMethod.PUT, requestEntity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }

    /**
     *  Test TTL on jwt Tokens. Expired tokens must not be accepted by the APIs verifier.
     * @throws TokenExpiredException
     */
    @Test
    public void testJWTTokenParsing() throws TokenExpiredException {
        assertThrows(TokenExpiredException.class, () -> {

        Date expiresAt = new Date(System.currentTimeMillis() - 60 * 1000); // Past date
        String jwt = JWT.create()
                .withIssuer("SABI-server module")
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC512("secret"));

        DecodedJWT decoded = JWT.decode(jwt);

        DecodedJWT verified = JWT.require(Algorithm.HMAC512("secret"))
                .withIssuer("SABI-server module")
                .build()
                .verify(jwt);
    });
    }

    @Test // REST-API
    public void testUserProfileUpdateWithNewMeasurementReminder() throws Exception {

        // given a successful update answer
        UserTo testuser = new UserTo(TestDataFactory.TESTUSER_EMAIL1, "testuser", "123");
        testuser.setId(1L);
        UserProfileTo updatedUserProfileTo = testDataFactory.getUserProfileToWithMeasurementReminderFor(testuser);

        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(updatedUserProfileTo, info);
        given(this.userService.updateProfile(updatedUserProfileTo, TestDataFactory.TESTUSER_EMAIL1)).willReturn(userProfileResultTo);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending an update request
        String requestJson = objectMapper.writeValueAsString(updatedUserProfileTo);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.USER_PROFILE.getPath(), HttpMethod.PUT, requestEntity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
        UserProfileTo retrievedUserProfileTo = objectMapper.readValue(responseEntity.getBody(), UserProfileTo.class);

        assertThat("Missing reminder entry",
                retrievedUserProfileTo.getMeasurementReminderTos().containsAll(updatedUserProfileTo.getMeasurementReminderTos()));
    }

    /**
     * Tests userprofile Rest API:  retrieve Users Profile
     *
     * @throws Exception
     */
    @Test // REST-API
    public void testGetUsersProfile() throws Exception {

        // given a successful retrieval answer
        UserProfileTo userProfileTo = testDataFactory.getBasicUserProfileTo();
        Message info = Message.info(CommonMessageCodes.OK);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        given(this.userService.getUserProfile(TestDataFactory.TESTUSER_EMAIL1)).willReturn(userProfileResultTo);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending a retrieval request
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.USER_PROFILE.getPath(), HttpMethod.GET, requestEntity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
