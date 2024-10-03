/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.PlagueCenterService;
import de.bluewhale.sabi.util.RestHelper;
import de.bluewhale.sabi.util.TestDataFactory;
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

import java.util.ArrayList;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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
@Tag("APITest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PlagueCenterControllerTest {
// ------------------------------ FIELDS ------------------------------

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


    final static String MOCKED_USER = "testsabi@bluewhale.de";

    @MockBean
    UserRepository userRepository;

    @MockBean
    PlagueCenterService plagueCenterService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

    TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private TestRestTemplate restTemplate;

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
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the resttemplate will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/plagues/status/list/de", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and some plague status
        PlagueStatusTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), PlagueStatusTo[].class);
        assertTrue("Did not received mockd Plague Status",myObjects.length>0);

    }
}
