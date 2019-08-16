/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.config;

import de.bluewhale.sabi.webclient.security.SabiDoorKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Provides the configuration for our JWT Token based Security.
 *
 * @author Stefan Schubert
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    SabiDoorKeeper sabiAuthenticationManager;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // require all requests to be authenticated except for the resources
        http.authorizeRequests()
                .antMatchers("/javax.faces.resource/**").permitAll()
                // Allow Welcome Page
                .antMatchers(HttpMethod.GET,"/", "/index.xhtml", "/register.xhtml", "/logout.xhtml").permitAll()
                // Allow Monitoring Endpoint
                .antMatchers(HttpMethod.GET,"/actuator/**").permitAll()

                // all others require authentication
                .anyRequest().authenticated();
        // login - using this the browser redirect to this page if login is required and you are not logged in.
        http.formLogin().loginPage("/login.xhtml").permitAll()
                .failureUrl("/login.xhtml?error=true").successForwardUrl("/sec/userportal.xhtml");
        // logout - back to login, you may specify a logout confirmation page with delayed redirect.
        http.logout().logoutSuccessUrl("/logout.xhtml");
        // not needed as JSF 2.2 is implicitly protected against CSRF
        http.csrf().disable();

    }

    @Autowired
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.sabiAuthenticationManager);
    }

}
