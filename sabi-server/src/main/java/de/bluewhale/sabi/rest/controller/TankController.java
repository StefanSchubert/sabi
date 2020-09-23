/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

/**
 * Author: Stefan Schubert
 * Date: 16.06.17
 */
@RestController
@RequestMapping(value = "api/tank")
@Slf4j
public class TankController {

// ------------------------------ FIELDS ------------------------------

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

// -------------------------- OTHER METHODS --------------------------

    @ApiOperation(value = "List all tanks belonging to calling user.", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_ACCEPTED,
                    message = "Success tanks returned.",
                    response = AquariumTo.class, responseContainer = "List"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<AquariumTo>> listUsersTanks(@RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        log.debug("Request Tank list for ",principal.getName());
        List<AquariumTo> aquariumToList = tankService.listTanks(principal.getName());
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Read details of a specific tank.", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_OK,
                    message = "Success tank returned.", response = AquariumTo.class),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AquariumTo> listUsersTanks(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                           @PathVariable(value = "id", required = true)
                                                           @ApiParam(name = "id", value = "id of your aquarium..") String id,
                                                           Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        AquariumTo aquariumTo = tankService.getTank(Long.valueOf(id), principal.getName());
        return new ResponseEntity<>(aquariumTo, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a tank from users profile.", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_OK,
                    message = "Tank deleted", response = HttpStatus.class),
            @ApiResponse(code = HttpURLConnection.HTTP_CONFLICT,
                    message = "Tank does not exists or does not belong to requesting user.", response = HttpStatus.class),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> removeTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                     @PathVariable(value = "id", required = true)
                                                     @ApiParam(name = "id", value = "id of your aquarium..") String id,
                                                     Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> resultTo = tankService.removeTank(Long.valueOf(id), principal.getName());

        ResponseEntity<String> responseEntity;

        if (resultTo.getMessage().getType().equals(Message.CATEGORY.INFO)) {
            responseEntity = new ResponseEntity<>("", HttpStatus.OK);
        } else {
            responseEntity = new ResponseEntity<>("", HttpStatus.CONFLICT);
        }

        return responseEntity;
    }

    @ApiOperation(value="Add a new tank to users profile.")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created - Remember Id of returned tank if you want to update it afterwards or retrieve it via list operation.",
                    response = AquariumTo.class),
            @ApiResponse(code = 409, message = "AlreadyCreated - A tank with this Id has already been created. Create double called?."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/create"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<AquariumTo> registerNewTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                      @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo createdAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(createdAquarium, HttpStatus.CREATED);
        } else {
            String msg="A Tank with Id " + aquariumTo.getId() + " already exist.";
            log.warn("Cannot create twice: "+msg);
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @ApiOperation(value="Update a specific tank.",notes="Tank data needs to be provides via json body.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Aquarium has been updated",response = AquariumTo.class),
            @ApiResponse(code = 409, message = "Something wrong - Tank ID does not exists or something like that."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<AquariumTo> updateTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                 @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> aquariumToResultTo = tankService.updateTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo updatedAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(updatedAquarium, HttpStatus.OK);
        } else {
            log.warn("Could not update tank: "+resultMessage.toString());
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}