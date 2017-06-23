/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This Class filters the /login request route.
 *
 * @author Stefan Schubert
 */
public class JWTLoginFilter extends AbstractAuthenticationProcessingFilter {
// --------------------------- CONSTRUCTORS ---------------------------

    public JWTLoginFilter(String url, AuthenticationManager authManager) {
        super(new AntPathRequestMatcher(url));
        setAuthenticationManager(authManager);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Responsible to extract username and password from the login request and
     * to delegate the auth attempt to the AuthenticationManager
     * @param pRequest
     * @param pResponse
     * @return
     * @throws AuthenticationException
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest pRequest,
                                                HttpServletResponse pResponse)
            throws AuthenticationException, IOException, ServletException {
        AccountCredentials loginData = new ObjectMapper()
                .readValue(pRequest.getInputStream(), AccountCredentials.class);

        // This constructur leaves the authenticated property to false
        UsernamePasswordAuthenticationToken yetUnConfirmedCredentials = new UsernamePasswordAuthenticationToken(
                loginData.getUsername(),
                loginData.getPassword()
        );
        return getAuthenticationManager().authenticate(
                yetUnConfirmedCredentials
        );
    }

    /**
     * Called automatically when the user was successful authenticated.
     *
     * <b>BUG-NOTICE: The current spring-security (4.2.2-RELEASE) API has a bug here! successful authentication is not
     *  decided upon the {@link Authentication#isAuthenticated()} flag! This callback
     *  will be invoked, as long as an {@link org.springframework.security.authentication.AuthenticationProvider#authenticate(Authentication)}
     *  call (i.e. {@link SabiDoorKeeper#authenticate(Authentication)}) will return an {@link Authentication} Object.
     *  Only when it returns <i>NULL</i> or throughs an exception the callback will omitted.
     * </b>
     *
     * @param pRequest
     * @param pResponse
     * @param chain
     * @param auth
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void successfulAuthentication(
            HttpServletRequest pRequest,
            HttpServletResponse pResponse, FilterChain chain,
            Authentication auth) throws IOException, ServletException {
        if (auth.isAuthenticated()) {
            TokenAuthenticationService.addAuthentication(pResponse, auth.getName());
        } else {
            throw new javax.security.sasl.AuthenticationException("Authentication Object was not authenticated!");
        }
    }
}
