/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Configuration;

/**
 * My current spring-boot provides jackson2.x which has no native support for conversion of
 * java 8 datetime type, that we we needed to add additional modules und register them by this config.
 * See https://github.com/FasterXML/jackson-modules-java8 for details.
 * @author Stefan Schubert
 */
@Configuration
public class JacksonConfiguration {

    ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule()); // new module, NOT JSR310Module

}
