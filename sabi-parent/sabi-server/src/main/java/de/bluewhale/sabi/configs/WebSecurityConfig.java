/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.security.JWTLoginFilter;
import de.bluewhale.sabi.security.SabiDoorKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
        http
            .csrf()
                .disable()
            .sessionManagement()
                // This will turn off creating sessions (as without it you would get a JSESSION-ID Cookie
                // However we provide REST Services here, having an additional session would be absurd.
                // NOTICE: With this it is pointless to use the SecurityContextHolder of Spring-Security,
                //         as without session it won't keep Information there.
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            .authorizeRequests()
                // Allow Welcome Page
                .antMatchers(HttpMethod.GET,"/", "/index.html").permitAll()
                // Allow Swagger api-doc access
                .antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources",
                        "/configuration/security", "/swagger-ui.html", "/webjars/**",
                        "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security").permitAll()
                // Registration and Login are accessible without JWT based authentication
                .antMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/req_pwd_reset").permitAll()
                .antMatchers(HttpMethod.GET, "/api/auth/email/**").permitAll()
                // Open statistics
                .antMatchers(HttpMethod.GET, "/api/stats/healthcheck").permitAll()
                // all others require JWT authentication
                .anyRequest().authenticated()
                .and()
            // JWT based authentication by POST of {"username":"<name>","password":"<password>"} which sets the
            // token header upon authentication
            .addFilterBefore(new JWTLoginFilter("/api/auth/login", authenticationManager()),
                        UsernamePasswordAuthenticationFilter.class);
            // And filter other requests to check the presence of JWT in header
           //  .addFilterBefore(new JWTAuthenticationFilter(),
              //          UsernamePasswordAuthenticationFilter.class);

    }

    @Autowired
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.sabiAuthenticationManager);
    }

}
