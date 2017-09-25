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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

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

    @Value("${captcha.check.url}")
    String captchaService;

    @ApiOperation("/login")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - extract user Token from header for further requests.", response = HttpStatus.class),
            @ApiResponse(code = 401, message = "Unauthorized - response won't contain a valid user token.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/login"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void loginUser(@RequestBody AccountCredentialsTo loginData) {

        // NOTICE: This Code is never reached, it solely purpose is to satisfy our api doc, so that we have the
        // function documented. The real login is being processed by our JWTLoginFilter which has been
        // configured as request filter for the /login path.
        userService.signIn(loginData.getUsername(), loginData.getPassword());

    }

    @ApiOperation("/register")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created - extract user Token from header for further requests.", response = UserTo.class),
            @ApiResponse(code = 412, message = "Captcha Validation code was invalid. Registration failed.", response = HttpStatus.class),
            @ApiResponse(code = 503, message = "Backend-Service not available, please try again later.", response = HttpStatus.class),
            @ApiResponse(code = 409, message = "Conflict - Username already exists.", response = UserTo.class)
    })
    @RequestMapping(value = {"/register"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<UserTo> createUser(@RequestBody UserTo pUserTo) {

        boolean validCaptcha = false;
        ResponseEntity<UserTo> responseEntity = null;

        // Step One: Sort out the Robots, before doing a single database request,
        // by checking the captcha code.
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map params = new HashMap<String, String>(1);
            params.put("code", pUserTo.getCaptchaCode());
            String checkURI = captchaService + "/{code}";
            final String checkresult = restTemplate.getForObject(checkURI, String.class, params);
            if ("Accepted".equals(checkresult)) {
                validCaptcha = true;
            } else {
                responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.PRECONDITION_FAILED);
            }
        } catch (RestClientException e) {
            responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (validCaptcha) {

            // Step two: try to register after sorting out the robots
            final ResultTo<UserTo> userToResultTo = userService.registerNewUser(pUserTo);

            final Message resultMessage = userToResultTo.getMessage();
            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
                responseEntity = new ResponseEntity<UserTo>(userToResultTo.getValue(), HttpStatus.CREATED);

                // TODO StS 29.08.15: Send the email delivering the validation token. Which language? UserTO or browser?
            } else {
                // TODO STS (17.06.16): Replace with Logging
                System.out.println("A User with eMail " + pUserTo.getEmail() + " already exist.");
                responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.CONFLICT);
            }

        }
        return responseEntity;
    }
}