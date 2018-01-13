/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - invalid token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<AquariumTo>> listUsersTanks(@RequestHeader(name = "Authorization", required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        String user = principal.getName();
        List<AquariumTo> aquariumToList = tankService.listTanks(user);
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);
    }

    @ApiOperation("/create")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created - Remember Id of returned tank if you want to update it afterwards or retrieve it via list operation."),
            @ApiResponse(code = 409, message = "AlreadyCreated - A tank with this Id has already been created. Create double called?."),
            @ApiResponse(code = 401, message = "Unauthorized - response won't contain a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/create"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AquariumTo> registerNewTank(@RequestHeader(name = "Authorization", required = true) String token,
                                                      @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        String user = principal.getName();
        ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo createdAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(createdAquarium, HttpStatus.CREATED);
        } else {
            // TODO STS (17.06.16): Replace with Logging
            System.out.println("A Tank with Id " + aquariumTo.getId() + " already exist.");
            responseEntity = new ResponseEntity<AquariumTo>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @ApiOperation("/update")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Aquarium has been updated"),
            @ApiResponse(code = 409, message = "Something wrong - Tank ID does not exists or something like that."),
            @ApiResponse(code = 401, message = "Unauthorized - response won't contain a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/update"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AquariumTo> updateTank(@RequestHeader(name = "Authorization", required = true) String token,
                                                 @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        String user = principal.getName();
        ResultTo<AquariumTo> aquariumToResultTo = tankService.updateTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo updatedAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(updatedAquarium, HttpStatus.OK);
        } else {
            // TODO STS (17.06.16): Replace with Logging
            responseEntity = new ResponseEntity<AquariumTo>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}