/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.security.JWTAuthorizationFilter;
import de.bluewhale.sabi.security.JWTLoginFilter;
import de.bluewhale.sabi.security.SabiDoorKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Provides the configuration for our JWT Token based Security.
 * <p>
 * Notice: This class was formerly an implemantation of WebSecurityConfigurerAdapter as of < Spring-boot 2.7.0
 * With Spring-Boot 3 the security configuration has been reworked.
 * With Spring-Boot 4 / Spring Security 7, MvcRequestMatcher and HandlerMappingIntrospector were removed.
 * requestMatchers() now uses PathPatternRequestMatcher internally by default.
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
                // ...existing code...
                .csrf(AbstractHttpConfigurer::disable)
                // ...existing code...
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Allow Welcome Page
                        .requestMatchers(HttpMethod.GET, "/").permitAll()
                        .requestMatchers(HttpMethod.GET, "/index.html").permitAll()
                        // Allow Monitoring Endpoint
                        .requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                        // Allow OAS3 api-doc access
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        // Registration and Login are accessible without JWT based authentication
                        .requestMatchers(HttpMethod.POST, Endpoint.LOGIN.getPath()).permitAll()
                        .requestMatchers(HttpMethod.POST, Endpoint.REGISTER.getPath()).permitAll()
                        .requestMatchers(HttpMethod.POST, Endpoint.PW_RESET_REQUEST.getPath()).permitAll()
                        .requestMatchers(HttpMethod.POST, Endpoint.PW_RESET.getPath()).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/email/**").permitAll()
                        // OIDC Login (sabi-150) – token validation is done inside the controller
                        .requestMatchers(HttpMethod.POST, Endpoint.OIDC_GOOGLE_AUTH.getPath()).permitAll()
                        // Open statistics
                        .requestMatchers(HttpMethod.GET, Endpoint.HEALTH_STATS.getPath()).permitAll()
                        .requestMatchers(HttpMethod.GET, Endpoint.PARTICIPANT_STATS.getPath()).permitAll()
                        .requestMatchers(HttpMethod.GET, Endpoint.TANK_STATS.getPath()).permitAll()
                        .requestMatchers(HttpMethod.GET, Endpoint.MEASUREMENT_STATS.getPath()).permitAll()
                        .requestMatchers(HttpMethod.GET, Endpoint.PLAGUE_STATS.getPath()).permitAll()
                        // Motd can be requested before login
                        .requestMatchers(HttpMethod.GET, "/api/app/motd/**").permitAll()
                        // Allow IOT Endpoints, they are checked internally based on specific API-Keys
                        .requestMatchers(HttpMethod.POST, Endpoint.IOT_API.getPath() + "/**").permitAll()
                        // Admin endpoints require ADMIN role (T062 — 002-fish-stock-catalogue)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // all others require JWT authentication
                        .anyRequest().authenticated()
                )
                // ...existing code...
                .addFilterBefore(new JWTLoginFilter(Endpoint.LOGIN.getPath(), sabiAuthenticationProvider),
                        UsernamePasswordAuthenticationFilter.class)
                // And filter other requests to check the presence of a valid JWT in header
                .addFilter(new JWTAuthorizationFilter(sabiAuthenticationProvider))
                .authenticationProvider(sabiAuthenticationProvider);

        return http.build();
    }


}
