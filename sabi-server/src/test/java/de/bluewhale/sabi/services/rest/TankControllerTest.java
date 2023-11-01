/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.TestDataFactory;
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
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Demonstrate usage of the tank API.
 *
 * @author Stefan Schubert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TankControllerTest {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

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
    @Autowired
    private TestRestTemplate restTemplate;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
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
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the resttemplate will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test aquarium
        AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
        boolean contained = false;
        for (AquariumTo aquarium : myObjects) {
            if (aquarium.equals(aquariumTo)) {
                contained = true;
                break;
            }
        }
        assertTrue("Did not received mockd Aquarium",contained);

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

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
        userTo.setId(1L);

        UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity aquariumEntity = aquariumMapper.mapAquariumTo2Entity(aquariumTo);
        aquariumEntity.setUser(userEntity); // ToMappper does not Map the User

        given(this.aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), userTo.getId())).willReturn(aquariumEntity);

        // and we need a valid authentication token for oure mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the resttemplate will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/" + aquariumTo.getId(),
                HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // and our test aquarium
        AquariumTo myObject = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
        assertEquals(myObject.getDescription(), aquariumTo.getDescription());
    }


    @Test
    public void testCreateUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
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
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        String requestJson = objectMapper.writeValueAsString(aquariumTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("/api/tank/create", entity, String.class);

        // then we should get a 201 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // and our test aquarium
        AquariumTo createdAquarium = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
        assertEquals(createdAquarium.getDescription(), aquariumTo.getDescription());
    }

    @Test
    public void testRemoveUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
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
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        String requestJson = objectMapper.writeValueAsString(aquariumTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/" + aquariumTo.getId(), HttpMethod.DELETE, entity, String.class);

        // then we should get a 201 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }


    @Test
    public void testUpdateUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
        userTo.setId(1L);

        UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);
        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        // This represents to TO/Entity bevore the update
        AquariumTo updatableAquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity updatableAquariumEntity =  aquariumMapper.mapAquariumTo2Entity(updatableAquariumTo);
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
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);

        updatableAquariumTo.setDescription(updateTestString);
        String requestJson = objectMapper.writeValueAsString(updatableAquariumTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank", HttpMethod.PUT, entity, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // and our test aquarium
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
        HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(AUTH_TOKEN, TOKEN_PREFIX + authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 401 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));

    }

}
