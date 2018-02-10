/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.AquariumDao;
import de.bluewhale.sabi.persistence.dao.MeasurementDao;
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
 * Demonstrate usage of the measurement API.
 *
 * @author Stefan Schubert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MeasurementControllerTest {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

    @MockBean
    UserDao userDao;

    @MockBean
    AquariumDao aquariumDao;

    @MockBean
    MeasurementDao measurementDao;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

    TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private TestRestTemplate restTemplate;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListUsersMeasurements() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo();
        userTo.setEmail(MOCKED_USER);
        userTo.setId(1L);
        given(this.userDao.loadUserByEmail(MOCKED_USER)).willReturn(userTo);


        AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(userTo);
        MeasurementTo measurementTo = testDataFactory.getTestMeasurementTo(aquariumTo.getId());

        List<MeasurementTo> testMeasurements = new ArrayList<>(1);
        testMeasurements.add(measurementTo);
        given(this.measurementDao.findUsersMeasurements(userTo.getId())).willReturn(testMeasurements);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/measurement/list", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test aquarium
        MeasurementTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), MeasurementTo[].class);
        assertThat(Arrays.asList(myObjects), hasItem(measurementTo));

    }

}
