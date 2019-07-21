/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;

import javax.inject.Named;

/**
 * Responsible to handle auth and profile operations that are directly related to the user.
 *
 * @author Stefan Schubert
 */
@Named
public class UserServiceImpl implements UserService {
    @Override
    public ResultTo<UserTo> registerNewUser(NewRegistrationTO newUser) {
        throw new UnsupportedOperationException("de.bluewhale.sabi.model.ResultTo<de.bluewhale.sabi.model.UserTo> registerNewUser([newUser])");
    }

    @Override
    public ResultTo<String> signIn(String pEmail, String pClearTextPassword) {
        throw new UnsupportedOperationException("de.bluewhale.sabi.model.ResultTo<java.lang.String> signIn([pEmail, pClearTextPassword])");
    }

    @Override
    public void requestPasswordReset(RequestNewPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void requestPasswordReset([requestData])");
    }

    @Override
    public void resetPassword(ResetPasswordTo requestData) throws BusinessException {
        throw new UnsupportedOperationException("void resetPassword([requestData])");
    }
}
