/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.config;

import de.bluewhale.sabi.webclient.security.SabiDoorKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Provides the configuration for our JWT Token based Security.
 *
 * Notice: This class was formerly an implemantation of WebSecurityConfigurerAdapter as of < Spring-boot 2.7.0
 * With Spring-Boot 3 the security configuration has been reworked,
 * see: https://www.springcloud.io/post/2022-03/spring-security-without-the-websecurityconfigureradapter/#gsc.tab=0
 * for details.
 *
 * @author Stefan Schubert
 */
@Configuration
public class WebSecurityConfig {

    @Autowired
    SabiDoorKeeper sabiAuthenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers("/jakarta.faces.resource/**").permitAll()
            // Allow Pages that don't require an auth context.
            .antMatchers("/", "/robots.txt","/sitemap.xml","/index.xhtml", "/register.xhtml",
                    "/pwreset.xhtml", "/preregistration.xhtml", "/logout.xhtml","/sessionExpired.xhtml",
                    "/credits.xhtml","/static/**","/.well-known/**").permitAll()
            // Allow Monitoring Endpoint
            .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            // all others require authentication
            .anyRequest().authenticated()

            .and()

            // In Case of a session timeout don't go directly to the login page,
            // use this page instead, for being able to notify the user what has happened.
            .sessionManagement().invalidSessionUrl("/sessionExpired.xhtml")

            .and()

            // login - using this the browser redirect to this page if login is required and you are not logged in.
            .formLogin().loginPage("/login.xhtml").permitAll()
            .failureUrl("/login.xhtml?error=true").successForwardUrl("/secured/userportal.xhtml")

            .and()

            // logout - back to login, you may specify a logout confirmation page with delayed redirect.
            .logout().logoutSuccessUrl("/logout.xhtml")
            .and()
            .authenticationProvider(this.sabiAuthenticationProvider)

            // not needed as JSF 2.2 is implicitly protected against CSRF
            .csrf().disable();

    return http.build();
    }

}
