/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.MeasurementService;
import de.bluewhale.sabi.services.TankService;
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
import java.util.Collections;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

;

@RestController
@RequestMapping(value = "api/measurement")
@Slf4j
public class MeasurementController {

// ------------------------------ FIELDS ------------------------------

    @Autowired
    MeasurementService measurementService;

    @Autowired
    TankService tankService;

// -------------------------- OTHER METHODS --------------------------

    @Operation(method = "Add a new measurement. Needs to be provided via json body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created-Remember Id of returned measurement if you want to update it afterwards or retrieve it via list operation."
                    ),
            @ApiResponse(responseCode = "409", description = "AlreadyCreated-A measurement with this Id has already been created.Create called doubled?"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized-request did not contained a valid user token.")
                    })
            @RequestMapping(value = {""}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
            @ResponseStatus(HttpStatus.CREATED)
            @ResponseBody
            public ResponseEntity<MeasurementTo>addMeasurement(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @RequestBody MeasurementTo measurementTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<MeasurementTo> measurementResultTo = measurementService.addMeasurement(measurementTo, principal.getName());

        ResponseEntity<MeasurementTo> responseEntity;
        final Message resultMessage = measurementResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            MeasurementTo createdMeasurement = measurementResultTo.getValue();
            responseEntity = new ResponseEntity<>(createdMeasurement, HttpStatus.CREATED);
        } else {
            String msg = "Measurement cannot be added twice. A Measurement with Id " + measurementTo.getId() + " already exist.";
            log.warn(msg);
            responseEntity = new ResponseEntity<>(measurementTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @Operation(method = "Correct an existing measurement. Needs to be provided via json body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK-Measurement has been updated"),
            @ApiResponse(responseCode = "409", description = "Something wrong-Measurement ID does not exists or something like that."),
                    @ApiResponse(responseCode = "401", description = "Unauthorized-request did not contained a valid user token.")
                    })
            @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
            @ResponseStatus(HttpStatus.OK)
            @ResponseBody
            public ResponseEntity<MeasurementTo>updateMeasurement(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
            @RequestBody MeasurementTo measurementTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<MeasurementTo> measurementResultTo = measurementService.updateMeasurement(measurementTo, principal.getName());

        ResponseEntity<MeasurementTo> responseEntity;
        final Message resultMessage = measurementResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            MeasurementTo updatedMeasurement = measurementResultTo.getValue();
            responseEntity = new ResponseEntity<>(updatedMeasurement, HttpStatus.OK);
        } else {
            log.warn("Measurementupdate failed. {}", resultMessage.toString());
            responseEntity = new ResponseEntity<>(measurementTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @Operation(method = "Lists measurements taken by the user. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success - list of all users measurements returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/list/{maxResultCount}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<MeasurementTo>> listUsersMeasurements(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                                     @PathVariable(value = "maxResultCount", required = false)
                                                                     @Parameter(name = "maxResultCount", description = "If provided only the latest maxResultCount recorded measurements will be returned. If ommittet you will get them all.") String maxResultCount,
                                                                     Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.

        Integer resultLimit = 0;
        if (maxResultCount != null) {
            try {
                resultLimit = Integer.valueOf(maxResultCount);
            } catch (NumberFormatException e) {
                resultLimit = 0;
            }
        }

        List<MeasurementTo> measurementToList = measurementService.listMeasurements(principal.getName(), resultLimit);
        return new ResponseEntity<>(measurementToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "List measurements belonging to a specific tank. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Success - list of tanks measurements returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized-request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/tank/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<MeasurementTo>> listUsersTankMeasurements(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                                         @PathVariable(value = "id", required = true)
                                                                         @Parameter(name = "id", description = "id of your aquarium..") String id,
                                                                         Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.

        Long pTankID;
        try {
            pTankID = Long.valueOf(id);
        } catch (NumberFormatException e) {
            log.warn("API Request sent with wrong TankID {}", e);
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        // We need to be sure if provided Tank does belong to the user.
        AquariumTo aquariumTo = tankService.getTank(pTankID, principal.getName());

        if (aquariumTo == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        List<MeasurementTo> MeasurementToList = measurementService.listMeasurements(pTankID);
        return new ResponseEntity<>(MeasurementToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "List measurements belonging to a specific tank and measurement unit. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description= "Success - list of tanks measurements for requested measurement unit returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized-request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/tank/{tankid}/unit/{unitid}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<MeasurementTo>> listUsersTankMeasurementsOfSpecificUnit(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                                                       @PathVariable(value = "tankid", required = true)
                                                                                       @Parameter(name = "tankid", description = "id of your aquarium..") String tankID,
                                                                                       @PathVariable(value = "unitid", required = true)
                                                                                       @Parameter(name = "unitid", description = "id of interested unit") String unitID,
                                                                                       Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.


        Long pTankID;
        Integer pUnitID;
        try {
            pTankID = Long.valueOf(tankID);
            pUnitID = Integer.valueOf(unitID);
        } catch (NumberFormatException e) {
            log.warn("API Request sent with wrong TankID or UnitID. {}", e);
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        // We need to be sure if provided Tank does belong to the user.
        AquariumTo aquariumTo = tankService.getTank(pTankID, principal.getName());

        if (aquariumTo == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        List<MeasurementTo> MeasurementToList = measurementService.listMeasurementsFilteredBy(pTankID, pUnitID);
        return new ResponseEntity<>(MeasurementToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "Drop a specific measurement. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description= "Measurement deleted"),
            @ApiResponse(responseCode = "409",
                    description= "Measurement does not exists or does not belong to requesting user."),
            @ApiResponse(responseCode = "401", description = "Unauthorized-request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> removeMeasurement(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                    @PathVariable(value = "id", required = true)
                                                    @Parameter(name = "id", description = "id of your measurement.") String id,
                                                    Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<MeasurementTo> resultTo = measurementService.removeMeasurement(Long.valueOf(id), principal.getName());

        ResponseEntity<String> responseEntity;

        if (resultTo.getMessage().getType().equals(Message.CATEGORY.INFO)) {
            responseEntity = new ResponseEntity<>("", HttpStatus.OK);
        } else {
            responseEntity = new ResponseEntity<>("", HttpStatus.CONFLICT);
        }

        return responseEntity;
    }
}