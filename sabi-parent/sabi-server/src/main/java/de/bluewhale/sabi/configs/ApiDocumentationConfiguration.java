/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.configs;

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
                    // .paths(regex("/api/*"))
                    .build()
                .pathMapping("/")
                .apiInfo(metadata());
    }


    private ApiInfo metadata() {
        return new ApiInfoBuilder()
                .title("sabi's REST API documentation")
                .description("Seawater Aquarium Business Intelligence (sabi) aims to gain insights from aquarium hobbyist " +
                        "for aquarium hobbyist according seawater measures.\n\n" +
                        "WELCOME! Your are looking at the API-Documentation of the project. Feel free" +
                        " to use it for developing your own client. However through it's open source, I" +
                        "plan to protect my backend systems by make usage of APIKeys. So if you indeed" +
                        "develop against my backend system, take care to contact me ahead and request an" +
                        "APIKey - even it I haven't implemented the protection yet, as the protection might be" +
                        "in place fast if required.\n\n" +
                        "For more detailed information (e.g. if you like to help) see https://github.com/StefanSchubert/sabi")
                .version("1.0")
                .license("MIT Licence (MIT)")
                .licenseUrl("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")
                .contact(MY_CONTACT_DATA)
                .build();
    }

}
