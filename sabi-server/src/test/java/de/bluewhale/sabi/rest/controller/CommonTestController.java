/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;

/**
 * Contains stuff that each ControllerTests uses
 */
public abstract class CommonTestController {

	@LocalServerPort
	private int port;

	protected RestClient restClient;

	@BeforeEach
	public void initRestClient() {
		if (restClient == null) {
			String url = String.format("http://localhost:%d/sabi", port);
			restClient = RestClient
					.builder()
					.baseUrl(url) // Dynamischer Port
					.build();
		}
	}

	protected TestDataFactory testDataFactory = TestDataFactory.getInstance();

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

}
