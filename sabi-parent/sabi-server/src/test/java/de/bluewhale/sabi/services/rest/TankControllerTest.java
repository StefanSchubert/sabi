/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.bluewhale.sabi.util.Mapper.mapAquariumTo2Entity;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Demonstrate usage of the tank API.
 *
 * @author Stefan Schubert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TankControllerTest {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

    @MockBean
    UserDao userDao;

    @MockBean
    AquariumDao aquariumDao;

    @Autowired
    ObjectMapper objectMapper;  // json mapper
    TestDataFactory testDataFactory = TestDataFactory.getInstance();
    @Autowired
    private TestRestTemplate restTemplate;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);

        List<AquariumTo> testAquariums = new ArrayList<>(1);
        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        testAquariums.add(aquariumTo);

        given(this.aquariumDao.findUsersTanks(userTo.getId())).willReturn(testAquariums);

        // and we need a valid authentication token for oure mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the resttemplate will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test aquarium
        AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
        assertThat(Arrays.asList(myObjects), hasItem(aquariumTo));

    }

    @Test
    public void testGetUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);

        given(this.aquariumDao.getUsersAquarium(aquariumTo.getId(), userTo.getId())).willReturn(aquariumTo);

        // and we need a valid authentication token for oure mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the resttemplate will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/" + aquariumTo.getId(),
                HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));

        // and our test aquarium
        AquariumTo myObject = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
        assertEquals(myObject.getDescription(),aquariumTo.getDescription());
    }


    @Test
    public void testCreateUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);


        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity createdAquariumEntity = new AquariumEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userTo.getId());
        createdAquariumEntity.setUser(userEntity);

        mapAquariumTo2Entity(aquariumTo, createdAquariumEntity);

        given(this.aquariumDao.create(any())).willReturn(createdAquariumEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests to create a aquarium
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

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

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity existingAquariumEntity = new AquariumEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userTo.getId());
        existingAquariumEntity.setUser(userEntity);

        mapAquariumTo2Entity(aquariumTo, existingAquariumEntity);

        given(this.aquariumDao.getUsersAquarium(aquariumTo.getId(), userTo.getId())).willReturn(aquariumTo);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests to create a aquarium
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

        String requestJson = objectMapper.writeValueAsString(aquariumTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/" + aquariumTo.getId(), HttpMethod.DELETE, entity, String.class);

        // then we should get a 201 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }


    @Test
    public void testUpdateUsersTank() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);


        AquariumTo updatableAquariumTo = testDataFactory.getTestAquariumFor(userTo);
        AquariumEntity updatableAquariumEntity = new AquariumEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userTo.getId());
        updatableAquariumEntity.setUser(userEntity);
        mapAquariumTo2Entity(updatableAquariumTo, updatableAquariumEntity);

        AquariumEntity updatedAquariumEntity = new AquariumEntity();
        updatedAquariumEntity.setUser(userEntity);
        mapAquariumTo2Entity(updatableAquariumTo, updatedAquariumEntity);
        String updateTestString = "Updated";
        updatedAquariumEntity.setDescription(updateTestString); // we test only on description in this test

        given(aquariumDao.getUsersAquarium(updatableAquariumTo.getId(), userTo.getId())).willReturn(updatableAquariumTo);
        given(aquariumDao.find(updatableAquariumTo.getId())).willReturn(updatableAquariumEntity);
        given(aquariumDao.update(updatableAquariumEntity)).willReturn(updatedAquariumEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests to update an aquarium
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

        updatableAquariumTo.setDescription(updateTestString);
        String requestJson = objectMapper.writeValueAsString(updatableAquariumTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank",HttpMethod.PUT, entity, String.class);

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
        headers.add("Authorization", "Bearer " + authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 401 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));

    }

}
