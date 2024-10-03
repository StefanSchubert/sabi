/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.ParameterMapper;
import de.bluewhale.sabi.mapper.UnitMapper;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.LocalizedUnitEntity;
import de.bluewhale.sabi.persistence.model.ParameterEntity;
import de.bluewhale.sabi.persistence.model.UnitEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.LocalizedUnitRepository;
import de.bluewhale.sabi.persistence.repositories.ParameterRepository;
import de.bluewhale.sabi.persistence.repositories.UnitRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
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
import java.util.Arrays;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Demonstrate usage of the unit REST API.
 * NOTICE: This test mocks the DAO persistent layer, as it was not meant to run as an integration test.
 * <p>
 * However notice the following drawbacks:
 * <p>
 * (1) It still requires the database, as without it we get a java.lang.IllegalStateException:
 * Failed to load ApplicationContext, though this might be fixed by proper test configuration
 * (2) Lines of code! The mocked variant outweighs the implementation by far. Which slows down development progress.
 * I leave it to demonstrate the effect. For those cases it would be much better to leave this as real integration
 * tests (however against an H2 in memory database, or by manually control your test data).
 *
 * @author Stefan Schubert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UnitControllerTest {
// ------------------------------ FIELDS ------------------------------

    final static String MOCKED_USER = "testsabi@bluewhale.de";

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
    UnitRepository unitRepository;
    @MockBean
    LocalizedUnitRepository localizedUnitRepository;
    @MockBean
    ParameterRepository parameterRepository;
    @MockBean
    UserRepository userRepository;

    @Autowired
    UserMapper userMapper;

    @Autowired
    UnitMapper unitMapper;

    @Autowired
    ParameterMapper parameterMapper;

    @Autowired
    ObjectMapper objectMapper;  // json mapper
    TestDataFactory testDataFactory = TestDataFactory.getInstance();
    @Autowired
    private TokenAuthenticationService encryptionService;
    @Autowired
    private TestRestTemplate restTemplate;

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void testListAvailableUnits() throws Exception {
        // given some Testdata via mocking
        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
        userTo.setId(1L);

        UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        UnitTo unitTo = testDataFactory.getTestUnitTo();

        UnitEntity unitEntity = unitMapper.mapUnitToEntity(unitTo);
        LocalizedUnitEntity localizedUnitEntity = new LocalizedUnitEntity();
        localizedUnitEntity.setUnitId(unitEntity.getId());
        localizedUnitEntity.setDescription(unitTo.getDescription());
        localizedUnitEntity.setLanguage("en");
        unitEntity.setLocalizedUnitEntities(List.of(localizedUnitEntity));

        List<UnitEntity> unitEntityList = new ArrayList<UnitEntity>();
        unitEntityList.add(unitEntity);

        given(this.unitRepository.findAll()).willReturn(unitEntityList);
        given(this.localizedUnitRepository.findByLanguageAndUnitId("en", unitEntity.getId())).willReturn(localizedUnitEntity);


        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // when this authorized user requests the unit list
        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/units/list/en", HttpMethod.GET, requestEntity, String.class);

        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and our test measurement
        UnitTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), UnitTo[].class);
        assertThat(Arrays.asList(myObjects), hasItem(unitTo));
    }

    @Test
    public void testFetchUnitParameterInfo() throws Exception {
        // Given
        Long usersTankID = 1l;

        // and some mocked data
        UserTo userTo = new UserTo(MOCKED_USER,"MockerUser","pw123");
        userTo.setId(1L);

        UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

        given(this.userRepository.getByEmail(MOCKED_USER)).willReturn(userEntity);

        ParameterTo parameterTo = testDataFactory.getTestParameterTo();
        ParameterEntity parameterEntity = parameterMapper.mapParameterTo2Entity(parameterTo);

        given(this.parameterRepository.findByBelongingUnitIdEquals(testDataFactory.getTestUnitTo().getId())).willReturn(parameterEntity);

        // and we need a valid authentication token for our mocked user
        String authToken = TokenAuthenticationService.createAuthorizationTokenFor(MOCKED_USER);

        // When this authorized user, requests detail parameter for a specific unit

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(authToken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Notice the that the controller defines a list, the rest-template will get it as array.
        ResponseEntity<String> responseEntity = restTemplate.exchange(Endpoint.UNITS + "/parameter/" + parameterEntity.getBelongingUnitId() + "/en", HttpMethod.GET, requestEntity, String.class);

        // Then
        // then we should get a 202 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
        // and our pre-stored test parameter
        ParameterTo storedParameter = objectMapper.readValue(responseEntity.getBody(), ParameterTo.class);
        assertThat("Prestored data changed?", storedParameter.getId().equals(parameterEntity.getId()));
    }

}
