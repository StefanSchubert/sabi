/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.CommonMessageCodes;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.UserService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;

/**
 * Checks UserProfile Rest API
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserProfileControllerTest {

    @MockBean
    UserService userService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

    TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @After
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
        UserProfileTo userProfileTo = testDataFactory.getUserProfileTo(4711L);
        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        given(this.userService.updateProfile(userProfileTo,"junit@maven.here")).willReturn(userProfileResultTo);

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
        UserProfileTo userProfileTo = testDataFactory.getUserProfileTo(4711L);
        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        given(this.userService.updateProfile(userProfileTo,TestDataFactory.TESTUSER_EMAIL1)).willReturn(userProfileResultTo);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(TestDataFactory.TESTUSER_EMAIL1);
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        // when - sending an update request
        String requestJson = objectMapper.writeValueAsString(userProfileTo);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.USER_PROFILE.getPath(), HttpMethod.PUT, requestEntity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }

}
