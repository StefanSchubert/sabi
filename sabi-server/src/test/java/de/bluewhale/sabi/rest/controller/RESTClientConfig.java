/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RESTClientConfig
{

	@Bean
	public RestClient provideRestClient(){
		return RestClient.builder()
				.baseUrl("https://jsonplaceholder.typicode.com")
				.build();
	}

}
