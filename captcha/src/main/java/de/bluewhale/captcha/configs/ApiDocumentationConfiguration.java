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
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Configures our Swagger usage
 *
 * @author Stefan Schubert
 */
@Configuration
public class ApiDocumentationConfiguration {

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
                .title("Bluewhale's Simple Captcha REST API documentation")
                .description("Currently included as part of https://github.com/StefanSchubert/sabi")
                .version("1.0")
                .license("MIT Licence (MIT)")
                .licenseUrl("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")
                .contact("Stefan.Schubert@bluewhale.de")
                .build();
    }

}
