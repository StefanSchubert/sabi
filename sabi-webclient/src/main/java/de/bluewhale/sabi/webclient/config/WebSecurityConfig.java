/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
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
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Provides the configuration for our JWT Token based Security.
 * <p>
 * Notice: This class was formerly an implementation of WebSecurityConfigurerAdapter as of < Spring-boot 2.7.0
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
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
        
        http
                .authorizeRequests(authorize -> authorize
                        .requestMatchers(mvcMatcherBuilder.pattern("/jakarta.faces.resource/**")).permitAll()
                        // Allow Pages that don't require an auth context.
                        .requestMatchers(mvcMatcherBuilder.pattern("/")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/robots.txt")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/sitemap.xml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/index.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/register.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/pwreset.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/preregistration.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/logout.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/sessionExpired.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/credits.xhtml")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/static/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern( "/.well-known/**")).permitAll()
                        // Allow Monitoring Endpoint
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/actuator/**")).permitAll()
                        // all others require authentication
                        .anyRequest().authenticated()

                )

                // In Case of a session timeout don't go directly to the login page,
                // use this page instead, for being able to notify the user what has happened.
                
                .sessionManagement(session -> session.invalidSessionUrl("/sessionExpired.xhtml"))

                // login - using this the browser redirect to this page if login is required and you are not logged in.
                .formLogin().loginPage("/login.xhtml").permitAll()
                .failureUrl("/login.xhtml?error=true").successForwardUrl("/secured/userportal.xhtml")

                .and()

                // logout - back to login, you may specify a logout confirmation page with delayed redirect.
                .logout().logoutSuccessUrl("/logout.xhtml")
                .and()
                .authenticationProvider(this.sabiAuthenticationProvider)

                // not needed as JSF 2.2 is implicitly protected against CSRF
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

}
