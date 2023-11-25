/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services.rest;

import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.services.AppService;
import de.bluewhale.sabi.util.RestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;


/**
 * Checks Motd Service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MotdControllerTest {

    @MockBean
    AppService appService;

    @Autowired
    private TestRestTemplate restTemplate;


    @AfterEach
    public void cleanUpMocks() {
        reset(appService);
    }

    /**
     * Tests MOTD Rest API in case we have no content.
     *
     * @throws Exception
     */
    @Test // REST-API
    public void testModtRetrievalWithNoNews() throws Exception {

        // given a motd
        MotdTo motdTo = new MotdTo("junit modt");

        HttpHeaders headers = RestHelper.buildHttpHeader();
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/app/motd/xx", HttpMethod.GET, null, String.class);

        // then we should get a 204 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    }

    /**
     * Tests MOTD Rest API in case we news.
     *
     * @throws Exception
     */
    @Test // REST-API
    public void testModtRetrieval() throws Exception {

        // given a motd
        String motd = "Junit Modt";
        given(this.appService.fetchMotdFor("en")).willReturn(motd);

        // when
        HttpHeaders headers = RestHelper.buildHttpHeader();
        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/app/motd/en", HttpMethod.GET, null, String.class);

        // then we should get a 200 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
