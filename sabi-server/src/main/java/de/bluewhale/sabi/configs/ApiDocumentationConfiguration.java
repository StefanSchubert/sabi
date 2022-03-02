/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures our Swagger usage
 *
 * @author Stefan Schubert
 */
@Configuration
public class ApiDocumentationConfiguration {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("sabi-public")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statsApi() {
        return GroupedOpenApi.builder()
                .group("sabi-stats-only")
                .pathsToMatch("/api/stats/**")
                .build();
    }

    @Bean
    public OpenAPI sabiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("sabi's REST API documentation")
                        .description("Seawater Aquarium Business Intelligence (sabi) aims to gain insights from aquarium hobbyist " +
                                "for aquarium hobbyist according seawater measures.\n\n" +
                                "WELCOME! Your are looking at the API-Documentation of the project. Feel free" +
                                " to use it for developing your own client. However though it's open source, I" +
                                "plan to protect my backend systems by make usage of APIKeys. So if you indeed " +
                                "develop against my backend system, take care to contact me ahead and request an" +
                                "APIKey - even it I haven't implemented the protection yet, the protection might be" +
                                "in place soon if required.\n\n" +
                                "For more detailed information (e.g. if you like to help) see https://github.com/StefanSchubert/sabi")
                        .version("1.1")


                        .license(new License().name("MIT Licence (MIT)")
                                .url("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")))
                ;
    }

}
