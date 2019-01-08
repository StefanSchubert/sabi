/*
 * Copyright (c) 2019 by Stefan Schubert
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.List;

/**
 * Author: Stefan Schubert
 * Date: 16.06.17
 */
@RestController
@RequestMapping(value = "api/tank")
public class TankController {

    static Logger logger = LoggerFactory.getLogger(TankController.class);

// ------------------------------ FIELDS ------------------------------

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

// -------------------------- OTHER METHODS --------------------------

    @ApiOperation(value = "/list", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
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
    public ResponseEntity<List<AquariumTo>> listUsersTanks(@RequestHeader(name = "Authorization", required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        List<AquariumTo> aquariumToList = tankService.listTanks(principal.getName());
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "/{id}", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_OK,
                    message = "Success tank returned.", response = AquariumTo.class),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - request did not contained a valid user token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AquariumTo> listUsersTanks(@RequestHeader(name = "Authorization", required = true) String token,
                                                           @PathVariable(value = "id", required = true)
                                                           @ApiParam(name = "id", value = "id of your aquarium..") String id,
                                                           Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        AquariumTo aquariumTo = tankService.getTank(Long.valueOf(id), principal.getName());
        return new ResponseEntity<>(aquariumTo, HttpStatus.OK);
    }

    @ApiOperation(value = "/{id}", notes = "You need to set the token issued by login or registration in the request header field 'Authorization'.")
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
    public ResponseEntity<String> removeTank(@RequestHeader(name = "Authorization", required = true) String token,
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

    @ApiOperation("/create")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created - Remember Id of returned tank if you want to update it afterwards or retrieve it via list operation.",
                    response = AquariumTo.class),
            @ApiResponse(code = 409, message = "AlreadyCreated - A tank with this Id has already been created. Create double called?."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/create"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<AquariumTo> registerNewTank(@RequestHeader(name = "Authorization", required = true) String token,
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
            logger.warn("Cannot create twice: "+msg);
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @ApiOperation("")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Aquarium has been updated",response = AquariumTo.class),
            @ApiResponse(code = 409, message = "Something wrong - Tank ID does not exists or something like that."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<AquariumTo> updateTank(@RequestHeader(name = "Authorization", required = true) String token,
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
            logger.warn("Could not update tank: "+resultMessage.toString());
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}