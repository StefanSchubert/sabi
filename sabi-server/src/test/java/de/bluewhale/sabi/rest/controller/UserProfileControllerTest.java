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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
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
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserProfileControllerTest extends CommonTestController {

    @MockBean
    UserService userService;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

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
        HttpHeaders httpHeader = RestHelper.buildHttpHeader();
        String requestJson = objectMapper.writeValueAsString(userProfileTo);

        try {
            ResponseEntity<String> responseEntity = restClient.put()
                    .uri(Endpoint.USER_PROFILE.getPath())
                    .headers(headers -> headers.addAll(httpHeader))  // Set headers
                    .body(requestJson)  // Set the request body
                    .retrieve()  // Executes the request and retrieves the response
                    .toEntity(String.class);  // Converts the response to a ResponseEntity

            // In case no exception is thrown, fail the test
            fail("Expected HttpClientErrorException$Forbidden to be thrown");

        } catch (HttpClientErrorException e) {
            // then we should get a 403 as result.
            assertThat("Spoofed access should produce a forbidden status.", e.getStatusCode().equals(HttpStatus.FORBIDDEN));
        }
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
        HttpHeaders httpHeaders = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending an update request
        String requestJson = objectMapper.writeValueAsString(userProfileTo);
        ResponseEntity<String> responseEntity = restClient.put()
                .uri(Endpoint.USER_PROFILE.getPath())
                .headers(headers -> headers.addAll(httpHeaders))  // Set headers
                .body(requestJson)  // Set the request body
                .retrieve()  // Executes the request and retrieves the response
                .onStatus(status -> status.value() != 200, (request, response) -> {
                    // then we should get a 200 as result.
                    throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
                })
                .toEntity(String.class);  // Converts the response to a ResponseEntity

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
        HttpHeaders httpHeaders = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending an update request
        String requestJson = objectMapper.writeValueAsString(updatedUserProfileTo);
        ResponseEntity<UserProfileTo> responseEntity = restClient.put()
                .uri(Endpoint.USER_PROFILE.getPath())
                .headers(headers -> headers.addAll(httpHeaders))  // Set headers
                .body(requestJson)  // Set the request body
                .retrieve()  // Executes the request and retrieves the response
                .onStatus(status -> status.value() != 200, (request, response) -> {
                    // then we should get a 200 as result.
                    throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
                })
                .toEntity(UserProfileTo.class);  // Converts the response to a ResponseEntity

         UserProfileTo retrievedUserProfileTo = responseEntity.getBody();

        // and should get a reminder within the profileTo
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
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending a retrieval request
        ResponseEntity<String> stringResponseEntity = restClient.get().uri(Endpoint.USER_PROFILE.getPath())
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .onStatus(status -> status.value() != 200, (request, response) -> {
                    // then we should get a 200 as result.
                    throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
                }).toEntity(String.class);
    }
}
