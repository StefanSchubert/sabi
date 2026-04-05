/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import tools.jackson.databind.json.JsonMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.CommonMessageCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.ReefDataExportService;
import de.bluewhale.sabi.services.UserService;
import de.bluewhale.sabi.util.RestHelper;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Checks UserProfile Rest API
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserProfileControllerTest extends CommonTestController {

    @MockitoBean
    UserService userService;

    @MockitoBean
    ReefDataExportService reefDataExportService;

    @Autowired
    JsonMapper objectMapper;  // json mapper

    @AfterEach
    public void cleanUpMocks() {
        reset(userService);
        reset(reefDataExportService);
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

    // -----------------------------------------------------------------------
    // T019 — Authenticated GET /export returns HTTP 200 + valid JSON
    // -----------------------------------------------------------------------

    @Test
    public void testAuthenticatedExportReturns200WithValidJson() throws Exception {
        // Given: mock returns a minimal but complete export document
        ReefDataExportTo exportTo = buildMinimalExport();
        given(reefDataExportService.buildExportForUser(TestDataFactory.TESTUSER_EMAIL1))
                .willReturn(exportTo);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        // When
        ResponseEntity<String> response = restClient.get()
                .uri(Endpoint.USER_PROFILE_EXPORT.getPath())
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .toEntity(String.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body, "Response body must not be null");

        // Must be parseable as JSON with top-level keys _meta and aquariums
        tools.jackson.databind.JsonNode root = objectMapper.readTree(body);
        assertNotNull(root.get("_meta"), "_meta key must be present");
        assertNotNull(root.get("aquariums"), "aquariums key must be present");
    }

    // -----------------------------------------------------------------------
    // T020 — Unauthenticated GET /export returns HTTP 401
    // -----------------------------------------------------------------------

    @Test
    public void testUnauthenticatedExportReturns401() {
        // Given: no Authorization header
        HttpHeaders plainHeader = RestHelper.buildHttpHeader();

        // When
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restClient.get()
                    .uri(Endpoint.USER_PROFILE_EXPORT.getPath())
                    .headers(headers -> headers.addAll(plainHeader))
                    .retrieve()
                    .toEntity(String.class);
        });

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString() == null || exception.getResponseBodyAsString().isEmpty(),
                "Response body must be empty for unauthorized export requests");
    }

    // -----------------------------------------------------------------------
    // T029 — PII absence: export JSON must not contain "email", "password", "username"
    // -----------------------------------------------------------------------

    @Test
    public void testExportContainsNoPii() throws Exception {
        ReefDataExportTo exportTo = buildMinimalExport();
        given(reefDataExportService.buildExportForUser(TestDataFactory.TESTUSER_EMAIL1))
                .willReturn(exportTo);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        ResponseEntity<String> response = restClient.get()
                .uri(Endpoint.USER_PROFILE_EXPORT.getPath())
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        assertNotNull(body);
        assertFalse(body.contains("\"email\""), "JSON must not contain key 'email' (SC-003)");
        assertFalse(body.contains("\"password\""), "JSON must not contain key 'password' (SC-003)");
        assertFalse(body.contains("\"username\""), "JSON must not contain key 'username' (SC-003)");
    }

    // -----------------------------------------------------------------------
    // T030 — _meta completeness: sabiSchemaVersion, exportedAt, description
    // -----------------------------------------------------------------------

    @Test
    public void testExportMetaBlockIsComplete() throws Exception {
        ReefDataExportTo exportTo = buildMinimalExport();
        given(reefDataExportService.buildExportForUser(TestDataFactory.TESTUSER_EMAIL1))
                .willReturn(exportTo);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        ResponseEntity<String> response = restClient.get()
                .uri(Endpoint.USER_PROFILE_EXPORT.getPath())
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .toEntity(String.class);

        tools.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
        tools.jackson.databind.JsonNode meta = root.get("_meta");
        assertNotNull(meta, "_meta must be present");

        // sabiSchemaVersion must equal "1.0"
        assertEquals("1.0", meta.get("sabiSchemaVersion").asText(), "sabiSchemaVersion must be 1.0");

        // exportedAt must match ISO-8601 UTC pattern
        String exportedAt = meta.get("exportedAt").asText();
        assertTrue(exportedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"),
                "exportedAt must be ISO-8601 UTC: " + exportedAt);

        // description must be non-blank
        String description = meta.get("description").asText();
        assertNotNull(description);
        assertFalse(description.isBlank(), "description must be non-blank");
    }

    // -----------------------------------------------------------------------
    // T031 — Aquarium structure: all five sub-array keys present
    // -----------------------------------------------------------------------

    @Test
    public void testExportAquariumStructureContainsAllSubArrayKeys() throws Exception {
        // Build export with one aquarium containing all five sub-arrays (may be empty)
        ReefDataExportTo exportTo = buildMinimalExportWithAquarium();
        given(reefDataExportService.buildExportForUser(TestDataFactory.TESTUSER_EMAIL1))
                .willReturn(exportTo);

        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        ResponseEntity<String> response = restClient.get()
                .uri(Endpoint.USER_PROFILE_EXPORT.getPath())
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .toEntity(String.class);

        tools.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
        tools.jackson.databind.JsonNode aquariums = root.get("aquariums");
        assertNotNull(aquariums, "aquariums array must be present");
        assertTrue(aquariums.isArray() && aquariums.size() > 0, "aquariums array must not be empty for this test");

        tools.jackson.databind.JsonNode firstAquarium = aquariums.get(0);
        assertNotNull(firstAquarium.get("measurements"), "measurements key must be present (SC-002)");
        assertNotNull(firstAquarium.get("plagueRecords"), "plagueRecords key must be present (SC-002)");
        assertNotNull(firstAquarium.get("fish"), "fish key must be present (SC-002)");
        assertNotNull(firstAquarium.get("corals"), "corals key must be present (SC-002)");
        assertNotNull(firstAquarium.get("treatments"), "treatments key must be present (SC-002)");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds a minimal ReefDataExportTo with valid _meta block and empty aquariums list. */
    private ReefDataExportTo buildMinimalExport() {
        de.bluewhale.sabi.model.ExportMetaTo meta = new de.bluewhale.sabi.model.ExportMetaTo();
        meta.setExportedAt(Instant.now().toString());
        meta.setSabiSchemaVersion(ReefDataExportTo.SCHEMA_VERSION);
        meta.setDescription("Sabi reef data export \u2014 test");

        ReefDataExportTo exportTo = new ReefDataExportTo();
        exportTo.setMeta(meta);
        exportTo.setAquariums(new ArrayList<>());
        return exportTo;
    }

    /** Builds a ReefDataExportTo with one aquarium that has all five sub-arrays (empty). */
    private ReefDataExportTo buildMinimalExportWithAquarium() {
        ReefDataExportTo exportTo = buildMinimalExport();
        de.bluewhale.sabi.model.AquariumExportTo aquarium = new de.bluewhale.sabi.model.AquariumExportTo();
        aquarium.setId(1L);
        aquarium.setDescription("Test Tank");
        aquarium.setWaterType("SEA_WATER");
        aquarium.setSize(300);
        aquarium.setSizeUnit("LITER");
        aquarium.setActive(true);
        aquarium.setMeasurements(new ArrayList<>());
        aquarium.setPlagueRecords(new ArrayList<>());
        aquarium.setFish(new ArrayList<>());
        aquarium.setCorals(new ArrayList<>());
        aquarium.setTreatments(new ArrayList<>());
        exportTo.getAquariums().add(aquarium);
        return exportTo;
    }
}
