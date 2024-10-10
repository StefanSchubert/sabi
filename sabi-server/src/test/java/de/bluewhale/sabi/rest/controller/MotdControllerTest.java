/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.services.AppService;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;


/**
 * Checks Motd Service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ModuleTest")
public class MotdControllerTest extends CommonTestController {

	@MockBean
	AppService appService;

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
				.retrieve() // führt den Request aus und ruft die Antwort ab
				.onStatus(
						status -> !status.isSameCodeAs(HttpStatus.NO_CONTENT),
						(request, response) -> {
							throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
						}
				)
				.toEntity(String.class);

	}


	/**
	 * Tests MOTD Rest API in case we have news.
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

		Assert.assertTrue(stringResponseEntity.toString().contains(motd));
	}

}
