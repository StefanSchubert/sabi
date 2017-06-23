/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.http.HTTPException;
import java.io.IOException;

/**
 * This Filter handles all other request which routes have been configured to be secure.
 * It will extract and parse the JWT-token (Validity check)
 *
 * @author Stefan Schubert
 */
public class JWTAuthenticationFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain filterChain)
            throws IOException, ServletException {

        Authentication authentication = TokenAuthenticationService
                .getAuthentication((HttpServletRequest)request);

        if (null == authentication) {
            throw new HTTPException(403);
        }

        // Usually we would do it this way for being able to probe the SecurityContext within our Service implementations.
        // However, this would be complete nonsense with a REST API design (not talking about RasperryPi as initial
        // lowcost, minimum RAM available deployment goal). So because we have switched off the session generation
        // in our WebSecurityConfig, spring won't keep any info stuffed into the SecurityContextHolder.
        // SecurityContextHolder.getContext()
        //         .setAuthentication(authentication);
        filterChain.doFilter(request,response);
    }

}
