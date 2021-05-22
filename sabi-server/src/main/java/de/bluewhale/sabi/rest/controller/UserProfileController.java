/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

@RestController
@RequestMapping(value = "api/userprofile")
@Slf4j
public class UserProfileController {

    @Autowired
    UserService userService;

    @ApiOperation(value = "Update an existing userProfile", notes = "Needs to be provided via json body.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK - Profile has been updated",
                    response = UserProfileTo.class),
            @ApiResponse(code = 409, message = "Something wrong - UserID does not exists or something like that."),
            @ApiResponse(code = 401, message = "Unauthorized - request did not contained a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<UserProfileTo> updateUserProfile(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                           @RequestBody UserProfileTo userProfileTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<UserProfileTo> userProfileResultTo = null;
        ResponseEntity<UserProfileTo> responseEntity;

        try {
            userProfileResultTo = userService.updateProfile(userProfileTo, principal.getName());
            final Message resultMessage = userProfileResultTo.getMessage();
            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
                UserProfileTo updatedUserProfileTo = userProfileResultTo.getValue();
                responseEntity = new ResponseEntity<>(updatedUserProfileTo, HttpStatus.OK);
            } else {
                log.error("Unexpected problem during update of userprofile occured.");
                responseEntity = new ResponseEntity<>(userProfileTo, HttpStatus.CONFLICT);
            }
        } catch (BusinessException e) {
            log.error("Userprofileupdate failed. {}", e.getMessage());
            e.printStackTrace();
            responseEntity = new ResponseEntity<>(userProfileTo, HttpStatus.CONFLICT);
        }

        return responseEntity;
    }
}