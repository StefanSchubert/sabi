/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.AuthExceptionCodes;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.services.CaptchaAdapter;
import de.bluewhale.sabi.services.NotificationService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

;

// If you seek an example, see
// http://websystique.com/springmvc/spring-mvc-4-restful-web-services-crud-example-resttemplate/
// and http://www.leveluplunch.com/java/tutorials/014-post-json-to-spring-rest-webservice/


/**
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
@RestController
@RequestMapping(value = "api/auth")
@Slf4j
public class AuthenticationController {

    @Autowired
    UserService userService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    CaptchaAdapter captchaAdapter;

    @Operation(method = "Login a user. Creates an authorization token for subsequent requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description =  "Accepted - extract user authorization token from header for further requests."),
            @ApiResponse(responseCode = "401", description =  "Unauthorized - response won't contain a valid user token.")
    })
    @RequestMapping(value = {"/login"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void loginUser(@RequestBody AccountCredentialsTo loginData) {

        // NOTICE: This Code is never reached, it solely purpose is to satisfy our api doc, so that we have the
        // function documented. The real login is being processed by our JWTLoginFilter which has been
        // configured as request filter for the /login path.
        userService.signIn(loginData.getUsername(), loginData.getPassword());

    }


    @Operation(method = "Reset users password. Legitimation and new password are transmitted via json body.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description =  "Accepted - password has been reset."),
            @ApiResponse(responseCode = "406", description =  "Not Acceptable - email is not registered."),
            @ApiResponse(responseCode = "503", description =  "Service temporarily unavailable  - Please retry later."),
            @ApiResponse(responseCode = "424", description =  "Failed Dependency - Invalid reset token. Please use token issued by email on reset request.")
    })
    @RequestMapping(value = {"/pwd_reset"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
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

    @Operation(method = "Request to reset users password. User will retrieve an email with instructions.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description =  "Accepted - email with reset token has been sent to user."),
            @ApiResponse(responseCode = "406", description =  "Not Acceptable - email is not registered."),
            @ApiResponse(responseCode = "424", description =  "Failed Dependency - Captcha failed. Please retry with another captcha."),
            @ApiResponse(responseCode = "503", description =  "Service temporarily unavailable  - Please retry later.")
    })
    @RequestMapping(value = {"/req_pwd_reset"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ResponseBody
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


    @Operation(method = "Used to validate a users email. User sends in a token which has been deliverd via email.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description =  "Accepted - User can proceed using this service by login now."),
            @ApiResponse(responseCode = "406", description =  "Not Acceptable - validation code or user unknown.")
    })
    @RequestMapping(value = {"/email/{email}/validation/{token}"}, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> validateUser(@PathVariable(value = "token", required = true)
                                               @Parameter(name = "token", description = "part of the link in validation email.") String validationToken,
                                               @PathVariable(value = "email", required = true)
                                               @Parameter(name = "email", description = "recipient of the link in validation email.") String email) {

        ResponseEntity<String> responseEntity;
        String responseHeadline;
        String responseText;

        boolean validated = userService.validateUser(email, validationToken);
        Locale usersLocale = fetchUsersLocale(email);
        ResourceBundle bundle = ResourceBundle.getBundle("i18n/RegistrationMessages", usersLocale);

        if (validated) {

            responseHeadline = bundle.getString("email.verify.successful.response.headline");
            responseText = bundle.getString("email.verify.successful.response.txt");

            responseEntity = new ResponseEntity<>(String.format(usersLocale, "<html><body><h1>%s</h1><p>%s</p></body></html>",
                    responseHeadline, responseText), HttpStatus.ACCEPTED);
            try {
                notificationService.sendWelcomeMail(email);
            } catch (MessagingException e) {
                log.error("Validation users email confirmation via token could not be send to the user. {}", e);
            }
        } else {
            responseHeadline = bundle.getString("email.verify.failed.response.headline");
            responseText = bundle.getString("email.verify.failed.response.txt");

            responseEntity = new ResponseEntity<>(String.format(Locale.ENGLISH, "<html><body><h1>%s</h1><p>%s</p></body></html>",
                    responseHeadline, responseText), HttpStatus.NOT_ACCEPTABLE);
        }

        return responseEntity;
    }

    private Locale fetchUsersLocale(String email) {
        Locale usersLocale;
        try {
            // fetch users locale to i18n response messages
            ResultTo<UserProfileTo> userProfile = userService.getUserProfile(email);
            usersLocale = (userProfile.getValue().getLanguage() == null ? Locale.ENGLISH : new Locale(userProfile.getValue().getLanguage()));
        } catch (BusinessException e) {
            log.error("Couldn't fetch validated user profile");
            usersLocale = Locale.ENGLISH;
        }
        return usersLocale;
    }

    @Operation(method = "Register a new User. User object needs to be transmitted via json body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description =  "Created - extract user Token from header for further requests."),
            @ApiResponse(responseCode = "412", description =  "Captcha Validation code was invalid. Registration failed."),
            @ApiResponse(responseCode = "503", description =  "Backend-Service not available, please try again later."),
            @ApiResponse(responseCode = "415", description =  "Wrong media type - Did you used a http header with MediaType=APPLICATION_JSON_VALUE ?"),
            @ApiResponse(responseCode = "409", description =  "Conflict - username and/or email-address already exists, or password too weak."),
            @ApiResponse(responseCode = "400", description =  "JSON Syntax invalid. Please check your paylod.")
    })
    @RequestMapping(value = {"/register"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserRegConfirmationTo> createUser(@RequestBody NewRegistrationTO pRegistrationUserTo) {

        Boolean validCaptcha;
        ResponseEntity<UserRegConfirmationTo> responseEntity = null;

        // Mapping of some values for the response object.
        UserRegConfirmationTo regConfirmationTo = new UserRegConfirmationTo(pRegistrationUserTo.getEmail(), pRegistrationUserTo.getUsername(),
                pRegistrationUserTo.getLanguage(), pRegistrationUserTo.getCountry());

        // Step One: Sort out the Robots, before doing a single database request,
        // by checking the captcha code.
        try {
            validCaptcha = captchaAdapter.isCaptchaValid(pRegistrationUserTo.getCaptchaCode());
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<UserRegConfirmationTo>(regConfirmationTo, HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (validCaptcha) {

            // Step two: try to register after sorting out the robots

            final ResultTo<UserTo> userToResultTo = userService.registerNewUser(pRegistrationUserTo);
            final UserTo createdUser = userToResultTo.getValue();
            final Message resultMessage = userToResultTo.getMessage();


            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {

                UserRegConfirmationTo userRegConfirmationTo = new UserRegConfirmationTo(createdUser.getEmail(),
                        createdUser.getUsername(), createdUser.getLanguage(), createdUser.getCountry());

                responseEntity = new ResponseEntity<UserRegConfirmationTo>(userRegConfirmationTo, HttpStatus.CREATED);

                try {
                    notificationService.sendValidationMail(createdUser);
                } catch (MessagingException e) {
                    log.error("Users registration incomplete and aborted, since notification mail coud not be sent. {}", e);
                    userService.unregisterUserAndClearPersonalData(pRegistrationUserTo.getEmail());
                    responseEntity = new ResponseEntity<UserRegConfirmationTo>(userRegConfirmationTo, HttpStatus.SERVICE_UNAVAILABLE);
                }

            } else {

                if (AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_EMAIL.equals(resultMessage.getCode())) {
                    String msg = "User registration failed. A User with eMail " + pRegistrationUserTo.getEmail() + " already exist.";
                    log.warn(msg);
                    responseEntity = new ResponseEntity<UserRegConfirmationTo>(regConfirmationTo, HttpStatus.CONFLICT);
                }

                if (AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_USERNAME.equals(resultMessage.getCode())) {
                    String msg = "User registration failed. A User with username " + pRegistrationUserTo.getUsername() + " already exist.";
                    log.warn(msg);
                    responseEntity = new ResponseEntity<UserRegConfirmationTo>(regConfirmationTo, HttpStatus.CONFLICT);
                }

                if (AuthMessageCodes.PASSWORD_TO_WEAK.equals(resultMessage.getCode())) {
                    String msg = "User Registration failed, Password too weak.";
                    log.warn(msg);
                    responseEntity = new ResponseEntity<UserRegConfirmationTo>(regConfirmationTo, HttpStatus.CONFLICT);
                }

            }
        } else {
            // Captcha invalid
            responseEntity = new ResponseEntity<UserRegConfirmationTo>(regConfirmationTo, HttpStatus.PRECONDITION_FAILED);
        }
        return responseEntity;
    }

}