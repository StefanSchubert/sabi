/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.PlagueRecordTo;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.model.PlagueTo;
import de.bluewhale.sabi.services.PlagueCenterService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;


@RestController
@RequestMapping(value = "api/plagues")
@Slf4j
public class PlagueCenterController {

// ------------------------------ FIELDS ------------------------------

    @Autowired
    UserService userService;

    @Autowired
    PlagueCenterService plagueCenterService;

// -------------------------- OTHER METHODS --------------------------

    @Operation(method = "List all plagues belonging to calling user tanks. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success plague records returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/record/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<PlagueRecordTo>> listUsersTanksPlagues(@RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        log.debug("Request tank plague list for {}",principal.getName());
        List<PlagueRecordTo> plagueRecordToList = plagueCenterService.listPlagueRecordsOf(principal.getName(), 0);
        return new ResponseEntity<>(plagueRecordToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "List plague status taxonomy in given language")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Success translated list of plague status returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = "/status/list/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<PlagueStatusTo>> listTranslatedPlagueStatus(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal,
            @PathVariable(value = "language", required = true)
            @Parameter(name = "language", description = "ISO-639-1 language code - used for i18n in communication.") String language) {

        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        log.debug("Request plague status list for {}",principal.getName());
        List<PlagueStatusTo> plagueStatusTos = plagueCenterService.listAllPlagueStatus(language);
        return new ResponseEntity<>(plagueStatusTos, HttpStatus.ACCEPTED);
    }

    @Operation(method = "List by sabi trackable plagues types in given language")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Success translated list of trackable plague returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = "/type/list/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<PlagueTo>> listTranslatedPlagues(
            @RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal,
            @PathVariable(value = "language", required = true)
            @Parameter(name = "language", description = "ISO-639-1 language code - used for i18n in communication.") String language) {

        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        log.debug("Request plague list for {}",principal.getName());
        List<PlagueTo> plagueTos = plagueCenterService.listAllPlagueTypes(language);
        return new ResponseEntity<>(plagueTos, HttpStatus.ACCEPTED);
    }

}