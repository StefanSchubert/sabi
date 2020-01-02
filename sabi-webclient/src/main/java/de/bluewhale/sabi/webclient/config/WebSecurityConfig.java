/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
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
        http
                .authorizeRequests()
                .antMatchers("/javax.faces.resource/**").permitAll()
                // Allow Pages that don't require an auth context.
                 .antMatchers("/", "/index.xhtml", "/register.xhtml",
                        "/preregistration.xhtml", "/logout.xhtml", "/credits.xhtml").permitAll()
                // Allow Monitoring Endpoint
                .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                // all others require authentication
                .anyRequest().authenticated()

                .and()
                
                // login - using this the browser redirect to this page if login is required and you are not logged in.
                .formLogin().loginPage("/login.xhtml").permitAll()
                .failureUrl("/login.xhtml?error=true").successForwardUrl("/secured/userportal.xhtml")

                .and()

                // logout - back to login, you may specify a logout confirmation page with delayed redirect.
                .logout().logoutSuccessUrl("/logout.xhtml")

                .and()

                // not needed as JSF 2.2 is implicitly protected against CSRF
                .csrf().disable();

    }

    @Autowired
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.sabiAuthenticationManager);
    }

}
