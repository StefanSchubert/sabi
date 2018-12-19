/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.MeasurementTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.MeasurementService;
import de.bluewhale.sabi.services.TankService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Author: Stefan Schubert
 * Date: 16.06.17
 */
@RestController
@RequestMapping(value = "api/measurement")
public class MeasurementController {

    static Logger logger = LoggerFactory.getLogger(MeasurementController.class);

// ------------------------------ FIELDS ------------------------------

    @Autowired
    MeasurementService measurementService;

    @Autowired
    TankService tankService;

// -------------------------- OTHER METHODS --------------------------

    @ApiOperation(value = "/list", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_ACCEPTED,
                    message = "Success - list of all users measurements returned.",
                    response = MeasurementTo.class, responseContainer = "List"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<MeasurementTo>> listUsersMeasurements(@RequestHeader(name = "Authorization", required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        List<MeasurementTo> MeasurementToList = measurementService.listMeasurements(principal.getName());
        return new ResponseEntity<>(MeasurementToList, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "/tank/{id}", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_OK,
                    message = "Success - list of tanks measurements returned.",
                    response = MeasurementTo.class, responseContainer = "List"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/tank/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<MeasurementTo>> listUsersTankMeasurements(@RequestHeader(name = "Authorization", required = true) String token,
                                                                         @PathVariable(value = "id", required = true)
                                                                         @ApiParam(name = "id", value = "id of your aquarium..") String id,
                                                                         Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.


        Long pTankID;
        try {
            pTankID = Long.valueOf(id);
        } catch (NumberFormatException e) {
            logger.warn("API Request sent with wrong TankID",e);
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

    @ApiOperation(value = "/{id}", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_OK,
                    message = "Measurement deleted", response = HttpStatus.class),
            @ApiResponse(code = HttpURLConnection.HTTP_CONFLICT,
                    message = "Measurement does not exists or does not belong to requesting user.", response = HttpStatus.class),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> removeMeasurement(@RequestHeader(name = "Authorization", required = true) String token,
                                                    @PathVariable(value = "id", required = true)
                                                    @ApiParam(name = "id", value = "id of your measurement.") String id,
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

    @ApiOperation("")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created - Remember Id of returned measurement if you want to update it afterwards or retrieve it via list operation.",
                    response = MeasurementTo.class),
            @ApiResponse(code = 409, message = "AlreadyCreated - A measurement with this Id has already been created. Create called doubled?"),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {""}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<MeasurementTo> addMeasurement(@RequestHeader(name = "Authorization", required = true) String token,
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
            String msg="Measurement cannot be added twice. A Measurement with Id " + measurementTo.getId() + " already exist.";
            logger.warn(msg);
            responseEntity = new ResponseEntity<>(measurementTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @ApiOperation("")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Measurement has been updated",
                    response = MeasurementTo.class),
            @ApiResponse(code = 409, message = "Something wrong - Measurement ID does not exists or something like that."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<MeasurementTo> updateMeasurement(@RequestHeader(name = "Authorization", required = true) String token,
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
            logger.warn("Measurementupdate failed. "+ resultMessage.toString());
            responseEntity = new ResponseEntity<>(measurementTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}