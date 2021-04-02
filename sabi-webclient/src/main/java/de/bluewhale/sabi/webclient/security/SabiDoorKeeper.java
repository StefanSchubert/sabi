/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.security;

import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.webclient.apigateway.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Involved only at Login stage. Does what the classic doorkeepers do...
 * Look deep into your eyes (ok, in this case your credentials) and
 * decides if you are to be trusted.
 *
 * @author Stefan Schubert
 */
@Service
@Configurable
@Slf4j
public class SabiDoorKeeper implements AuthenticationProvider {

    @Autowired
    UserService userService;

    @Override
    public Authentication authenticate(Authentication unconfirmedAuthentication) throws AuthenticationException {

        ResultTo<String> resultTo = null;

        try {
            // it happened that the auth req. was repeated though the first one was answered unconfirmed.
            // in the second call the credentials were null...leading to an NPE
            if (unconfirmedAuthentication.getPrincipal() != null && unconfirmedAuthentication.getCredentials() != null) {
                resultTo = userService.signIn(unconfirmedAuthentication.getPrincipal().toString(),
                        unconfirmedAuthentication.getCredentials().toString());
            }
        } catch (Exception e) {
            log.warn("AuthService failed! {}", e);
            throw new BadCredentialsException(e.getLocalizedMessage());
        }

        if (resultTo != null && resultTo.getMessage().getCode().equals(AuthMessageCodes.SIGNIN_SUCCEEDED)) {

            // This constructor set the authenticated property to true
            // (The API allows to set the property only through the constructor)
            UsernamePasswordAuthenticationToken confirmedAuthentication =
                    new UsernamePasswordAuthenticationToken(unconfirmedAuthentication.getPrincipal(),
                            unconfirmedAuthentication.getCredentials(), Collections.emptyList());

            return confirmedAuthentication;
        } else {
            log.info("AuthService detected failed login.");
            //return unconfirmedAuthentication;
            if (resultTo != null) {
                throw new BadCredentialsException(resultTo.getMessage().toString());
            } else {
                throw new BadCredentialsException("Authentication failed...");
            }
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
