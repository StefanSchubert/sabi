package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.rest.model.SabiAuthToken;
import de.bluewhale.sabi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
@RestController
@RequestMapping(value = "/user")
public class AuthenticationController {

    @Autowired
    UserService userService;

    @RequestMapping(value = { "/login" }, method = RequestMethod.GET)
    public SabiAuthToken loginUser(@RequestParam(value = "email", required = true,
            defaultValue = "0") String email,
                                   @RequestParam(value = "password", required = true,
                                           defaultValue = "0") String password) {

        SabiAuthToken sabiAuthToken = new SabiAuthToken();
        sabiAuthToken.setToken(UUID.fromString(email).toString());

        return sabiAuthToken;
    }

}