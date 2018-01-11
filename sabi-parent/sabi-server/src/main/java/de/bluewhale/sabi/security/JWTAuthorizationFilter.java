/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.security;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static de.bluewhale.sabi.security.TokenAuthenticationService.HEADER_STRING;
import static de.bluewhale.sabi.security.TokenAuthenticationService.TOKEN_PREFIX;

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
        String token = req.getHeader(HEADER_STRING);

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
            chain.doFilter(req, res);
        } else {
            // don't continue the chain
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setHeader("Authorization", token);

            res.setContentType(MediaType.TEXT_PLAIN.getType());
            PrintWriter writer = res.getWriter();
            writer.print("Access denied! You need to login and send the Token 'Authorization' issued through the response token after login in your request token." +
                    "See also API documentation  available under: /sabi/swagger-ui.html");

            // don't set content length , don't close
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(HEADER_STRING);
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
