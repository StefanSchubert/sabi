/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.configs;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.List;

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
