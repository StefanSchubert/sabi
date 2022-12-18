/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures our OpenAPI usage
 *
 * @author Stefan Schubert
 */
@Configuration
public class ApiDocumentationConfiguration {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("captcha-public")
                .pathsToMatch("/api/**")
                .build();
    }



    @Bean
    public OpenAPI captchaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simple Captcha Service to start with. Welcome to the REST API documentation.")
                        .description("Currently included as part of https://github.com/StefanSchubert/sabi")
                        .version("1.0")
                        .license(new License().name("MIT Licence (MIT)")
                                .url("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")))
                ;
    }
}
