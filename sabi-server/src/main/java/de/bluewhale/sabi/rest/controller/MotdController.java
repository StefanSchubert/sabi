/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.MotdTo;
import de.bluewhale.sabi.services.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

;

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

    @Operation(method="Retrieves the Message of today. Used by sabis operator to make announcements, such as planned maintenance work.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Motd retrieved."),
            @ApiResponse(responseCode = "204", description = "OK - but currently no motd provided."),
            @ApiResponse(responseCode = "404", description = "You tried to query with an empty language param."),
            @ApiResponse(responseCode = "503", description = "Backend service is currently unavailable.")
    })
    @RequestMapping(value = "/motd/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MotdTo> getTranslatedMotd(
            @PathVariable(value = "language", required = true)
            @Parameter(name = "language", description = "ISO-639-1 language code - used for i18n in communication.") String language) {

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
