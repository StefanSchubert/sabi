/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;
import static de.bluewhale.sabi.api.HttpHeader.TOKEN_PREFIX;

/**
 * Used by requests which requires an authentication.
 * Checks the presence of a valid JWT Token in the Request header field 'Authorization' following the Bearer schema.
 *
 * @author Stefan Schubert
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public JWTAuthorizationFilter(AuthenticationManager authManager) {
        super(authManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        String token = req.getHeader(AUTH_TOKEN);

        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication;
        try {
            authentication = getAuthentication(req);
        } catch (Exception e) {
            authentication = null;
        }

        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // NOTICE we also generate a new Token to renew tokens TTL.
            // This is necessary to extend the "backend-session" if users-client session continues.
            // it's in the responsibility of the client to continue with the renewed token (the old token will be still
            // valid until its TTL expired - for the cases where the client missed the cycle due to network problems.)
             String userID = (String) authentication.getPrincipal();
             TokenAuthenticationService.addAuthentication(res,userID);
            chain.doFilter(req, res);
        } else {
            // don't continue the chain
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // don't set content length , don't close
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN);
        if (token != null) {
            // parse the token.
            String user = TokenAuthenticationService.extractUserFromToken(token);

            if (user != null) {
                return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
            }
            return null;
        }
        return null;
    }
}
