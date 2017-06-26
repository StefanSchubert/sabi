/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AccountCredentialsTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// If you seek an example, see
// http://websystique.com/springmvc/spring-mvc-4-restful-web-services-crud-example-resttemplate/
// and http://www.leveluplunch.com/java/tutorials/014-post-json-to-spring-rest-webservice/


/**
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
@RestController
@RequestMapping(value = "api/auth")
public class AuthenticationController {

    @Autowired
    UserService userService;


    @ApiOperation("/login")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - extract user Token from header for further requests.", response = AccountCredentialsTo.class),
            @ApiResponse(code = 401, message = "Unauthorized - response won't contain a valid user token.", response = AccountCredentialsTo.class)
    })
    @RequestMapping(value = {"/login"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void loginUser(@RequestBody AccountCredentialsTo loginData) {

        // NOTICE: This Code is never reached, it solely purpose is to satisfy our api doc, so that we have the
        // function documented. The real login is being processed by our JWTLoginFilter which has been
        // configured as request filter for the /login path.
        final ResultTo<String> resultTo = userService.signIn(loginData.getUsername(), loginData.getPassword());

    }


    @RequestMapping(value = {"/register"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<UserTo> createUser(@RequestBody UserTo pUserTo) {

        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(pUserTo);

        final Message resultMessage = userToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            return new ResponseEntity<UserTo>(userToResultTo.getValue(), HttpStatus.CREATED);
        }
        else {
            // TODO STS (17.06.16): Replace with Logging
            System.out.println("A User with eMail " + pUserTo.getEmail() + " already exist.");
            return new ResponseEntity<UserTo>(pUserTo, HttpStatus.CONFLICT);
        }

    }

}