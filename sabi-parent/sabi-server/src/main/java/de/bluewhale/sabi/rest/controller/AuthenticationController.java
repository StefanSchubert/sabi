/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AccountCredentialsTo;
import de.bluewhale.sabi.model.RequestNewPasswordTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.services.AuthMessageCodes;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
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
    JavaMailSender mailer;

    @Value("${captcha.check.url}")
    String captchaService;

    @Value("${sabi.mailvalidation.url}")
    String mailValidationURL;

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


    @ApiOperation("/req_pwd_reset")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Accepted - email with reset token has been sent to user.", response = HttpStatus.class),
            @ApiResponse(code = 406, message = "Not Acceptable - email is not registered.", response = HttpStatus.class),
            @ApiResponse(code = 424, message = "Failed Dependency - Captcha failed. Please retry with another captcha.", response = HttpStatus.class)
    })
    @RequestMapping(value = {"/req_pwd_reset"}, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequestNewPasswordTo> requestPasswordReset(@RequestBody RequestNewPasswordTo requestData) {

        ResponseEntity<RequestNewPasswordTo> responseEntity = new ResponseEntity<>(requestData, HttpStatus.ACCEPTED);

        Map<AuthMessageCodes, HttpStatus> responseState = new HashMap<>();
        responseState.put(AuthMessageCodes.CORRUPTED_TOKEN_DETECTED, HttpStatus.FAILED_DEPENDENCY);
        responseState.put(AuthMessageCodes.EMAIL_NOT_REGISTERED, HttpStatus.NOT_ACCEPTABLE);

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
                sendWelcomeMail(email);
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
            // TODO STS (26.09.17): proper logging
            responseEntity = new ResponseEntity<UserTo>(pUserTo, HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (validCaptcha) {

            // Step two: try to register after sorting out the robots
            final ResultTo<UserTo> userToResultTo = userService.registerNewUser(pUserTo);
            final UserTo createdUser = userToResultTo.getValue();
            final Message resultMessage = userToResultTo.getMessage();
            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
                responseEntity = new ResponseEntity<UserTo>(createdUser, HttpStatus.CREATED);

                try {
                    sendValidationMail(createdUser);
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
        }
        return responseEntity;
    }

    private void sendValidationMail(UserTo createdUser) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(createdUser.getEmail());
        helper.setSubject("sabi Account Validation");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine (userTO) extract sabi target URL from application properties
        helper.setText("<html><body>" +
                "<h1>Welcome to sabi</h1>" +
                "<p>To activate your account and make use of sabi we require to verify your email-address." +
                "To do so, please click on the following link or copy paste it into your browser:</p>" +
                mailValidationURL + "/email/" + createdUser.getEmail() + "/validation/" + createdUser.getValidationToken() + "<br/ >" +
                "</body></html>", true);

        mailer.send(message);
    }

    private void sendWelcomeMail(String email) throws MessagingException {
        MimeMessage message = mailer.createMimeMessage();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject("Sabi account activated");
        helper.setFrom("no-reply@sabi.bluewhale.de");

        // todo i18n Textbausteine ggf. DISCLAIMER/ Nutzungsbedingungen
        helper.setText("<html><body>" +
                "<h1>Successfull registration</h1>" +
                "<p>Your account has been activated." +
                "you can now login into sabi with your credentials.</p>" +
                "</body></html>", true);

        mailer.send(message);
    }
}