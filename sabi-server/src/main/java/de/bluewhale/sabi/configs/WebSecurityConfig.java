/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Provides the configuration for our JWT Token based Security.
 * <p>
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
    public SecurityFilterChain filterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {

        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                // GitHub CodeQL complains here: Possible CSRF Risk.
                // However, though springs csrf mechanism is disabled does not mean this app has nor CSRF protection.
                // CSRF attacks are based on the "trust that a site has in a user's browser" (cited from
                // https://en.wikipedia.org/wiki/Cross-site_request_forgery).
                // See also demo attack on https://www.baeldung.com/spring-security-csrf
                // Springs mechanism is to inject additional header or hidden form fields which
                // are not controlled by the browser (cookie; basic auth etc..).
                // This App does something very similiar: see BE Token in Authorization Usecase here
                // https://github.com/StefanSchubert/sabi/wiki/06.-Runtime-View
                // So we do have a CSRF Protection implemented, just not the spring way.
                .csrf(csrf -> csrf.disable())
                // This will turn off creating sessions (as without it you would get a JSESSION-ID Cookie
                // However we provide REST Services here, having an additional session would be absurd.
                // NOTICE: With this it is pointless to use the SecurityContextHolder of Spring-Security,
                //         as without session it won't keep Information there.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Allow Welcome Page
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/index.html")).permitAll()
                        // Allow Monitoring Endpoint
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/actuator/**")).permitAll()

                        // Allow OAS3 api-doc access
                        .requestMatchers(mvcMatcherBuilder.pattern("/v3/api-docs/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/swagger-ui/**")).permitAll()
                        // Registration and Login are accessible without JWT based authentication
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, Endpoint.LOGIN.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, Endpoint.REGISTER.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, Endpoint.PW_RESET_REQUEST.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, Endpoint.PW_RESET.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/api/auth/email/**")).permitAll()
                        // Open statistics
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/api/stats/healthcheck")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, Endpoint.PARTICIPANT_STATS.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, Endpoint.TANK_STATS.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, Endpoint.MEASUREMENT_STATS.getPath())).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, Endpoint.PLAGUE_STATS.getPath())).permitAll()
                        // Motd can be requested before login
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/api/app/motd/**")).permitAll()
                        // Allow IOT Endpoints, they are checked internally based on specific API-Keys
                        .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, Endpoint.IOT_API.getPath() + "/**")).permitAll()
                        // all others require JWT authentication
                        .anyRequest().authenticated()
                )
                // JWT based authentication by POST of {"username":"<name>","password":"<password>"} which sets the
                // token header upon authentication
                .addFilterBefore(new JWTLoginFilter(Endpoint.LOGIN.getPath(), sabiAuthenticationProvider),
                        UsernamePasswordAuthenticationFilter.class)
                // And filter other requests to check the presence of a valid JWT in header
                .addFilter(new JWTAuthorizationFilter(sabiAuthenticationProvider))
                .authenticationProvider(sabiAuthenticationProvider);

        return http.build();
    }


}
