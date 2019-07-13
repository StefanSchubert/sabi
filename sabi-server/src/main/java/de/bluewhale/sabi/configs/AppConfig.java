/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.security.SabiDoorKeeper;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 *
 * User: Stefan Schubert
 * Date: 04.09.15
 */
@Configuration
@EnableSwagger2
@ComponentScan(basePackages = "de.bluewhale.sabi")
@PropertySource("classpath:application.properties")
public class AppConfig {

    /*
    Usage example: env.getProperty("testbean.name"), In case you need to inject something
    in bean declarations below.
     */
    @Autowired
    Environment env;

    @Bean
    public TokenAuthenticationService encryptionService() {
        // @Value for constructor params is to late, so these needed to be handled here.
        return new TokenAuthenticationService(env.getProperty("accessToken.SECRET"), env.getProperty("accessToken.TTL"));
    }

    // Required, so that Spring @Value know how to interpret ${}
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    // Used by Login-Door ;-)
    @Bean
    public SabiDoorKeeper sabiAuthenticationManager(){
        return new SabiDoorKeeper();
    } ;

}
