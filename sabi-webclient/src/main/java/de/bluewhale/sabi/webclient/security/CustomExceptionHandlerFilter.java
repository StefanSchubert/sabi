/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Because of our SpringSecurity config the standard spring exception handler conventional configuration
 * nor the ExceptionHandler Extensions which are based upon the Dispatcher Servlet, does
 * not work here because of the FacesServlet context.
 *
 * As a workaround we use a Filter we will add to the security filter chain instead.
 */
@Component
@Slf4j
public class CustomExceptionHandlerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

            if (log.isDebugEnabled()) {
                String requestStatus = (status != null) ? status.toString() : "Null";
                log.debug("Passing Custom Exception Filter with request status {}",requestStatus);
            }

            if (status != null) {
                HttpStatus httpStatus = HttpStatus.valueOf(status.toString());
                log.warn("There was a servlet problem. Returned httpStatus {}",httpStatus);
                switch (httpStatus) {
                    case HttpStatus.NOT_FOUND -> response.sendRedirect("/error");
                    case HttpStatus.INTERNAL_SERVER_ERROR -> response.sendRedirect("/error");
                }

            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Any error within the filter? Call the error Controller.
            log.error("Exception Filter could not access request due to previous error rerouting to error controller.",e);
            response.sendRedirect("/error");
        }
    }
}