/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.services.AppService;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;


/**
 * Checks Motd Service
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ModuleTest")
public class MotdControllerTest {

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
	AppService appService;

	@Autowired
	private RestClient restClient;

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
		given(this.appService.fetchMotdFor("xx")).willReturn(null);

		restClient.get().uri("/api/app/motd/xx")
				.retrieve()
				.onStatus(status -> status.value() != 204, (request, response) -> {
					throw new RuntimeException("Retrieved wrong status code: "+response.getStatusCode());
				});
	}


	/**
	 * Tests MOTD Rest API in case we news.
	 *
	 * @throws Exception
	 */
	@Test // REST-API
	public void testModtRetrieval() throws Exception {
		String motd = "Junit Modt";
		given(this.appService.fetchMotdFor("en")).willReturn(motd);

		ResponseEntity<String> stringResponseEntity = restClient.get().uri("/api/app/motd/en")
				.retrieve()
				.onStatus(status -> status.value() != 200, (request, response) -> {
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		Assert.assertEquals(motd,stringResponseEntity.toString());
	}

}
