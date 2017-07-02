/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Configures our Swagger usage
 *
 * @author Stefan Schubert
 */
@Configuration
public class ApiDocumentationConfiguration {

    private static final Contact MY_CONTACT_DATA = new Contact("Stefan Schubert", "", "Stefan.Schubert@bluewhale.de");


    @Bean
    public Docket documentation() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                    .apis(RequestHandlerSelectors.any())
                    .paths(PathSelectors.any())
                    .build()
                .pathMapping("/")
                // Suppress generation of default return codes, but have in mind, that you need to take care
                // of @APIResponses(...) on your controller then.
                .useDefaultResponseMessages(false)
                .apiInfo(metadata());
    }


    private ApiInfo metadata() {
        return new ApiInfoBuilder()
                .title("Simple Captcha Service to start with. Welcome to the REST API documentation.")
                // fixme change description when module separates from sabi.
                .description("Currently included as part of https://github.com/StefanSchubert/sabi")
                .version("1.0")
                .license("MIT Licence (MIT)")
                .licenseUrl("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")
                .contact(MY_CONTACT_DATA)
                .build();
    }

}
