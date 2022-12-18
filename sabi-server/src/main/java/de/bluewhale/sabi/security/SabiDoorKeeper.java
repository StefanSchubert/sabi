/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Involved only at Login stage.
 * Does what the classic doorkeepers do. Look deep into your eyes (ok, in this case your credentials) and
 * decides if you are to be trusted.
 *
 * @author Stefan Schubert
 */
@Service
@Configurable
public class SabiDoorKeeper implements AuthenticationProvider , AuthenticationManager {

    @Autowired
    UserService userService;

    @Override
    public Authentication authenticate(Authentication unconfirmedAuthentication) throws AuthenticationException {

        ResultTo<String> resultTo = userService.signIn(unconfirmedAuthentication.getPrincipal().toString(), unconfirmedAuthentication.getCredentials().toString());

        if (resultTo.getMessage().getCode().equals(AuthMessageCodes.SIGNIN_SUCCEEDED)) {

            String usersEmailAsID = resultTo.getValue();

            // This constructor set the authenticated property to true
            // (The API allows to set the property only through the constructor)
            UsernamePasswordAuthenticationToken confirmedAuthentication =
                    new UsernamePasswordAuthenticationToken(new SabiPrincipal(usersEmailAsID),
                            unconfirmedAuthentication.getCredentials(), Collections.emptyList());

            return confirmedAuthentication;
        } else {
            // return unconfirmedAuthentication;
            throw new BadCredentialsException(resultTo.getMessage().toString());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
