/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.AuthExceptionCodes;
import de.bluewhale.sabi.services.CaptchaAdapter;
import de.bluewhale.sabi.services.NotificationService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
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

    @Autowired
    NotificationService notificationService;

    @Autowired
    CaptchaAdapter captchaAdapter;

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


    @ApiOperation("/pwd_reset")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - password has been reset.", response = HttpStatus.class),
            @ApiResponse(code = 406, message = "Not Acceptable - email is not registered.", response = HttpStatus.class),
            @ApiResponse(code = 503, message = "Service temporarily unavailable  - Please retry later.", response = HttpStatus.class),
            @ApiResponse(code = 424, message = "Failed Dependency - Invalid reset token. Please use token issued by email on reset request.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/pwd_reset"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> requestPasswordReset(@RequestBody ResetPasswordTo requestData) {

        ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.ACCEPTED);

        Map<AuthExceptionCodes, HttpStatus> responseState = new HashMap<>();
        responseState.put(AuthExceptionCodes.AUTHENTICATION_FAILED, HttpStatus.FAILED_DEPENDENCY);
        responseState.put(AuthExceptionCodes.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        responseState.put(AuthExceptionCodes.USER_LOCKED, HttpStatus.NOT_ACCEPTABLE);

        try {
            userService.resetPassword(requestData);
        } catch (BusinessException e) {
            HttpStatus httpStatus = responseState.get(e.getCode());
            responseEntity = new ResponseEntity<>(requestData.toString(), (httpStatus == null ? HttpStatus.FAILED_DEPENDENCY : httpStatus));
        }

        return responseEntity;

    }

    @ApiOperation("/req_pwd_reset")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - email with reset token has been sent to user.", response = HttpStatus.class),
            @ApiResponse(code = 406, message = "Not Acceptable - email is not registered.", response = HttpStatus.class),
            @ApiResponse(code = 424, message = "Failed Dependency - Captcha failed. Please retry with another captcha.", response = HttpStatus.class),
            @ApiResponse(code = 503, message = "Service temporarily unavailable  - Please retry later.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/req_pwd_reset"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequestNewPasswordTo> requestPasswordReset(@RequestBody RequestNewPasswordTo requestData) {

        ResponseEntity<RequestNewPasswordTo> responseEntity = new ResponseEntity<>(requestData, HttpStatus.ACCEPTED);

        Map<AuthExceptionCodes, HttpStatus> responseState = new HashMap<>();
        responseState.put(AuthExceptionCodes.AUTHENTICATION_FAILED, HttpStatus.FAILED_DEPENDENCY);
        responseState.put(AuthExceptionCodes.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        responseState.put(AuthExceptionCodes.USER_LOCKED, HttpStatus.NOT_ACCEPTABLE);

        try {
            userService.requestPasswordReset(requestData);
        } catch (BusinessException e) {
            HttpStatus httpStatus = responseState.get(e.getCode());
            responseEntity = new ResponseEntity<>(requestData, (httpStatus == null ? HttpStatus.FAILED_DEPENDENCY : httpStatus));
        }

        return responseEntity;

    }


    @ApiOperation("/email/{email}/validation/{token}")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - User can proceed using this service by login now..", response = HttpStatus.class),
            @ApiResponse(code = 406, message = "Not Acceptable - validation code or user unknown.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/email/{email}/validation/{token}"}, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> validateUser(@PathVariable(value = "token", required = true)
                                               @ApiParam(name = "token", value = "part of the link in validation email.") String validationToken,
                                               @PathVariable(value = "email", required = true)
                                               @ApiParam(name = "email", value = "recipient of the link in validation email.") String email) {

        ResponseEntity<String> responseEntity;

        boolean validated = userService.validateUser(email, validationToken);

        // TODO STS (26.09.17): i18n of response
        if (validated) {
            responseEntity = new ResponseEntity<>("<html><body><h1>Welcome to sabi!</h1><p>Your email has been " +
                    "successfully validated. You can now login with your account. Have fun using sabi.</p></body></html>", HttpStatus.ACCEPTED);
            try {
                notificationService.sendWelcomeMail(email);
            } catch (MessagingException e) {
                e.printStackTrace();
                // TODO STS (26.09.17): Proper logging
            }
        } else {
            responseEntity = new ResponseEntity<>("<html><body><h1>Account validation failed!</h1><p>Your account is still locked." +
                    " Did copied the full validation link into your webbrowser? Please try again.</p></body></html>", HttpStatus.NOT_ACCEPTABLE);
        }

        return responseEntity;
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

        Boolean validCaptcha;
        ResponseEntity<UserTo> responseEntity = null;

        // Step One: Sort out the Robots, before doing a single database request,
        // by checking the captcha code.
        validCaptcha = captchaAdapter.isCaptchaValid(pUserTo.getCaptchaCode());
        if (validCaptcha == null) {
            return new ResponseEntity<UserTo>(pUserTo, HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (validCaptcha) {

            // Step two: try to register after sorting out the robots
            final ResultTo<UserTo> userToResultTo = userService.registerNewUser(pUserTo);
            final UserTo createdUser = userToResultTo.getValue();
            final Message resultMessage = userToResultTo.getMessage();
            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
                responseEntity = new ResponseEntity<UserTo>(createdUser, HttpStatus.CREATED);

                try {
                    notificationService.sendValidationMail(createdUser);
                } catch (MessagingException e) {
                    // TODO STS (26.09.17): proper logging
                    System.out.println(e);
                    responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.SERVICE_UNAVAILABLE);
                }

            } else {
                // TODO STS (17.06.16): Replace with Logging
                System.out.println("A User with eMail " + pUserTo.getEmail() + " already exist.");
                responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.CONFLICT);
            }
        } else {
            responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.PRECONDITION_FAILED);
        }
        return responseEntity;
    }

}