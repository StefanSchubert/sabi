/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.config;

import de.bluewhale.sabi.webclient.security.CustomExceptionHandlerFilter;
import de.bluewhale.sabi.webclient.security.SabiDoorKeeper;
import de.bluewhale.sabi.webclient.security.SabiOidcSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Provides the configuration for our JWT Token based Security.
 * <p>
 * Notice: This class was formerly an implementation of WebSecurityConfigurerAdapter as of < Spring-boot 2.7.0
 * With Spring-Boot 3 the security configuration has been reworked,
 * see: https://www.springcloud.io/post/2022-03/spring-security-without-the-websecurityconfigureradapter/#gsc.tab=0
 * With Spring-Boot 4 / Spring Security 7: MvcRequestMatcher removed → PathPatternRequestMatcher,
 * FilterSecurityInterceptor removed → AuthorizationFilter, authorizeRequests → authorizeHttpRequests.
 *
 * @author Stefan Schubert
 */
@Configuration
public class WebSecurityConfig {

    @Autowired
    SabiDoorKeeper sabiAuthenticationProvider;

    @Autowired
    private CustomExceptionHandlerFilter customExceptionHandlerFilter;

    @Autowired
    private SabiOidcSuccessHandler sabiOidcSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        http
                // Add our sabi-113 required exception handler here
                .addFilterAfter(customExceptionHandlerFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(path.matcher("/jakarta.faces.resource/**")).permitAll()
                        // Allow Pages that don't require an auth context.
                        .requestMatchers(path.matcher("/")).permitAll()
                        .requestMatchers(path.matcher("/robots.txt")).permitAll()
                        .requestMatchers(path.matcher("/sitemap.xml")).permitAll()
                        .requestMatchers(path.matcher("/index.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/register.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/gdpr.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/terms_of_usage.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/impressum.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/pwreset.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/preregistration.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/logout.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/credits.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/ipv6info.xhtml")).permitAll()
                        .requestMatchers(path.matcher("/static/error/**")).permitAll()
                        .requestMatchers(path.matcher("/.well-known/**")).permitAll()
                        .requestMatchers(path.matcher("/error")).permitAll() // Error Controller
                        .requestMatchers(path.matcher("/images/**")).permitAll()
                        // Public HouseReef report (no login required)
                        .requestMatchers(path.matcher("/houseReefReport.xhtml")).permitAll()
                        // Public photo proxy for HouseReef report (token-gated on backend side)
                        .requestMatchers(path.matcher("/api/public/report/**")).permitAll()
                        // OIDC login flow (sabi-150): Spring Security OAuth2 redirect URIs
                        .requestMatchers(path.matcher("/oauth2/**")).permitAll()
                        .requestMatchers(path.matcher("/login/oauth2/**")).permitAll()
                        // Allow Monitoring Endpoint
                        .requestMatchers(path.matcher(HttpMethod.GET, "/actuator/**")).permitAll()
                        // all others require authentication
                        .anyRequest().authenticated()
                )

                // On session timeout redirect back to index (splash) page.
                // For public pages (e.g. houseReefReport.xhtml) we redirect to the SAME URL so
                // that the token query parameter is preserved and the page renders on the next
                // request (with a fresh session). For all other URLs we redirect to "/" as before.
                //
                // IMPORTANT: request.getSession() MUST be called before sendRedirect to force the
                // servlet container to create a new session and send a fresh Set-Cookie header.
                // Without this, the browser retains the stale JSESSIONID → infinite redirect loop
                // (same root cause as Spring's SimpleRedirectInvalidSessionStrategy#createNewSession=true).
                .sessionManagement(session -> session.invalidSessionStrategy((request, response) -> {
                    // Create a fresh server-side session so the response carries a new JSESSIONID
                    // cookie, replacing the stale one that triggered this strategy.
                    request.getSession(true);
                    String uri = request.getRequestURI();
                    if (uri != null && uri.contains("houseReefReport")) {
                        // Preserve the original URL (incl. ?token=...) so the public report
                        // renders correctly after the new session has been established.
                        String redirectUrl = uri;
                        String query = request.getQueryString();
                        if (query != null) {
                            redirectUrl += "?" + query;
                        }
                        response.sendRedirect(redirectUrl);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/");
                    }
                }))

                // login - using this the browser redirect to this page if login is required and you are not logged in.
                // IMPORTANT: use defaultSuccessUrl (redirect) instead of successForwardUrl (forward)
                // because a servlet forward carries the login POST's ViewState into the target page,
                // causing JSF to attempt restoring mismatched component state → ArrayIndexOutOfBoundsException.
                .formLogin(form -> form
                        .loginPage("/login.xhtml").permitAll()
                        .failureUrl("/login.xhtml?error=true")
                        .defaultSuccessUrl("/secured/userportal.xhtml", true)
                )

                // OIDC Login via Google (sabi-150)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.xhtml")
                        .successHandler(sabiOidcSuccessHandler)
                        .failureUrl("/login.xhtml?error=oidc")
                )

                // logout - back to login, you may specify a logout confirmation page with delayed redirect.
                .logout(logout -> logout
                        .logoutSuccessUrl("/logout.xhtml")
                )

                .authenticationProvider(this.sabiAuthenticationProvider)

                // not needed as JSF 2.2 is implicitly protected against CSRF
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

}
