/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.PlagueCenterService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Demonstrate usage of the PlagueCenter API.
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PlagueCenterControllerTest extends CommonTestController {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

    @MockBean
    UserRepository userRepository;

    @MockBean
    PlagueCenterService plagueCenterService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    ObjectMapper objectMapper;  // json mapper


// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListTranslatedPlagueStatus() throws Exception {
        // given some Testdata via mocking

        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
        userTo.setId(1L);
        UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        List<PlagueStatusTo> plagueStatusTos = new ArrayList<>();
        PlagueStatusTo plagueStatusTo = new PlagueStatusTo();
        plagueStatusTo.setId(1);
        plagueStatusTo.setDescription("Junit Plague Status");
        plagueStatusTos.add(plagueStatusTo);

        given(this.plagueCenterService.listAllPlagueStatus("de")).willReturn(plagueStatusTos);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests his aquarium list
        HttpHeaders authedHeader = RestHelper.prepareAuthedHttpHeader(authToken);

        // Notice the that the controller defines a list, the restClient will get it as array.
        ResponseEntity<String> stringResponseEntity = restClient.get().uri(Endpoint.PLAGUE_CENTER_SERVICE.getPath() +"/status/list/de")
                .headers(headers -> headers.addAll(authedHeader))
                .retrieve()
                .onStatus(status -> status.value() != 202, (request, response) -> {
                    // then we should get a 202 as result.
                    throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
                }).toEntity(String.class);


        // and we should get some plague status
        PlagueStatusTo[] myObjects = objectMapper.readValue(stringResponseEntity.getBody(), PlagueStatusTo[].class);
        assertTrue("Did not received mockd Plague Status",myObjects.length>0);

    }
}
