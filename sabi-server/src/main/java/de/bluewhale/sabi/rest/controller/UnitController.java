/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.ParameterTo;
import de.bluewhale.sabi.model.UnitTo;
import de.bluewhale.sabi.services.MeasurementService;
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

;

@RestController
@RequestMapping(value = "/api/units")
@Slf4j
public class UnitController {

// ------------------------------ FIELDS ------------------------------

    @Autowired
    MeasurementService measurementService;

// -------------------------- OTHER METHODS --------------------------


    @Operation(method = "Lists all measurements units which are currently known by the backend. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success - list of all supported measurement units returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<UnitTo>> listAllMeasurementUnits(@RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        List<UnitTo> unitToList = measurementService.listAllMeasurementUnits();
        return new ResponseEntity<>(unitToList, HttpStatus.ACCEPTED);
    }


    @Operation(method = "Read details of a specific unit. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success detail parameter returned."),
            @ApiResponse(responseCode = "404",
                    description = "Not detail parameter available for requested unit."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/parameter/{unitID}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ParameterTo> readDetailParameterOfUnit(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                                 @PathVariable(value = "unitID", required = true)
                                                                 @Parameter(name = "unitID", description = "id of the unit you query details for...") Integer unitID,
                                                                 Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ParameterTo parameterTo = measurementService.fetchParameterInfoFor(unitID);
        if (parameterTo == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(parameterTo, HttpStatus.ACCEPTED);
        }
    }

}