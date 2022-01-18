/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Configures our Swagger usage
 *
 * @author Stefan Schubert
 */
@Configuration
@EnableSwagger2
public class ApiDocumentationConfiguration {

    private static final Contact MY_CONTACT_DATA = new Contact("Stefan Schubert", "", "Stefan.Schubert@bluewhale.de");

    @Bean
    public Docket documentation() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                    .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
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
                .license("MIT Licence (MIT)")
                .licenseUrl("https://github.com/StefanSchubert/sabi/blob/master/LICENSE")
                .contact(MY_CONTACT_DATA)
                .build();
    }

    /**
     * Springfox workaround required by Spring Boot 2.6
     * See https://github.com/springfox/springfox/issues/346
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                mappings.removeIf(mapping -> mapping.getPatternParser() != null);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

}
