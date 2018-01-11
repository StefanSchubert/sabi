/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.security.TokenAuthenticationService;
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
import java.util.List;

/**
 * Author: Stefan Schubert
 * Date: 16.06.17
 */
@RestController
@RequestMapping(value = "api/tank")
public class TankController {

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

    @ApiOperation(value="/list", notes ="You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(code = HttpURLConnection.HTTP_ACCEPTED,
                    message = "Success tanks returned.",
                    response = AquariumTo.class, responseContainer = "List"),
            @ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized - invalid token.",
                    response = String.class)
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<AquariumTo>> listUsersTanks(@RequestHeader(name = "Authorization", required = true) String token) {


        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // we use it here again to extract the user, for which we query the aquarium list.
        String user = TokenAuthenticationService.extractUserFromToken(token);


        // fixme Token-Security Check is being handled through spring-security
        final Long extractedUserId = 1L;
        List<AquariumTo> aquariumToList = tankService.listTanks(extractedUserId);
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);

    }

}