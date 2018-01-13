/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.UserDao;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;

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
        AquariumTo aquariumTo = getTestAquariumFor(userTo);
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
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list" , HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test aquarium
        AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
        assertThat(Arrays.asList(myObjects), hasItem(aquariumTo));

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
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/tank/list" , HttpMethod.GET, requestEntity, String.class);

        // then we should get a 401 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));

    }

    private AquariumTo getTestAquariumFor(UserTo userTo) {
        AquariumTo aquariumTo = new AquariumTo();
        aquariumTo.setActive(Boolean.TRUE);
        aquariumTo.setDescription("Test Tank");
        aquariumTo.setId(1L);
        aquariumTo.setSize(80);
        aquariumTo.setSizeUnit(SizeUnit.LITER);
        aquariumTo.setUserId(userTo.getId());
        return aquariumTo;
    }
}
