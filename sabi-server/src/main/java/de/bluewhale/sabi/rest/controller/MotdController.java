/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.services.AppService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Used to get the message of today.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/app")
@Slf4j
public class MotdController {


    @Autowired
    AppService appService;

    @ApiOperation(value="Retrieves the Message of today.",notes="Used by sabis operator to make announcements, such as planned maintenance work.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Motd retrieved.",
                    response = MotdTo.class),
            @ApiResponse(code = 204, message = "OK - but currently no motd provided.",
                    response = MotdTo.class),
            @ApiResponse(code = 404, message = "You tried to query with an empty language param.",
                    response = HttpStatus.class),
            @ApiResponse(code = 503, message = "Backend service is currently unavailable.")
    })
    @RequestMapping(value = "/motd/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MotdTo> getNewCaptchaChallenge(
            @PathVariable(value = "language", required = true)
            @ApiParam(name = "language", value = "ISO-639-1 language code - used for i18n in communication.") String language) {

        ResponseEntity<MotdTo> response;

        if (Strings.isEmpty(language)) language = "en"; // default

        try {
            String motd = appService.fetchMotdFor(language);
            if (Strings.isEmpty(motd)) {
                response = new ResponseEntity<>(new MotdTo(), HttpStatus.NO_CONTENT);
            } else {
                response = new ResponseEntity<>(new MotdTo(motd), HttpStatus.OK);
            }

        } catch (Exception e) {
            response = new ResponseEntity<>(new MotdTo(), HttpStatus.SERVICE_UNAVAILABLE);
        }

        return response;
    }


}
