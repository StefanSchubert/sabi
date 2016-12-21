package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.*;
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
@RequestMapping(value = "/user")
public class AuthenticationController {

    @Autowired
    UserService userService;


    @ApiOperation("/login")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "email", value ="Users eMail as ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "password", value ="Users password", required = true, dataType = "string")

    })
    @ApiResponses({
            @ApiResponse(code = 202, message = "Success - Extract user Token from Answer for further requests.", response = UserTo.class),
            @ApiResponse(code = 401, message = "Unauthorized - response won't contain a valid user token.", response = UserTo.class)
    })
    @RequestMapping(value = {"/login"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<UserTo> loginUser(@RequestParam(value = "email", required = true,
            defaultValue = "0") String email,
                                            @RequestParam(value = "password", required = true,
                                                    defaultValue = "0") String password) {


        final UserTo userTo = new UserTo(email, password);

        final ResultTo<String> resultTo = userService.signIn(email, password);
        if (resultTo != null &&
            resultTo.getMessage() != null &&
            Message.CATEGORY.INFO.equals(resultTo.getMessage().getType())) {
            userTo.setxAuthToken(resultTo.getValue());
            return new ResponseEntity<UserTo>(userTo, HttpStatus.ACCEPTED);
        }
        else {
            return new ResponseEntity<UserTo>(userTo, HttpStatus.UNAUTHORIZED);
        }
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