/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.security.SabiDoorKeeper;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * User: Stefan Schubert
 * Date: 04.09.15
 */
@Configuration
@ComponentScan(basePackages = "de.bluewhale.sabi")
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

    // Using BCrypt for better password security.
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

}
