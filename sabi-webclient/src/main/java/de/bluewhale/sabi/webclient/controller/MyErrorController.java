/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Nice try (to solve sabi-113) but It won't work for <b>jakarta.faces.application.ViewExpiredException</b>
 * as the Controller will be involved in the Context of the DispatcherServlet, which won't happen for errors
 * occurring in the JSF engine. The error which has been seen by sabi-113 us happening
 * in the Context of the FacesServlet which gets uncaught all way up through the springframework.security.eb Filterchain.
 * <p>
 * So though this Error-Controller configuration is more or less dead code because of the
 * Faces Techstack here, I will leave it with this explanation as reference in it.
 */
@Controller
@Slf4j
public class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "/static/error/404.html";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "/static/error/500.html";
            }
        }

        return "/static/error/error.html"; // fallback
    }
}