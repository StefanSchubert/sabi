/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.controller;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.*;

import javax.inject.Named;
import javax.validation.constraints.NotNull;

/**
 * TODO STS: Add Description here...
 *
 * @author Stefan Schubert
 */
@Named
public class UserServiceImpl implements UserService {
    @Override
    public @NotNull ResultTo<UserTo> registerNewUser(@NotNull NewRegistrationTO newUser) {
        return null;
    }

    @Override
    public @NotNull ResultTo<String> signIn(@NotNull String pEmail, @NotNull String pClearTextPassword) {
        return null;
    }

    @Override
    public void requestPasswordReset(@NotNull RequestNewPasswordTo requestData) throws BusinessException {

    }

    @Override
    public void resetPassword(@NotNull ResetPasswordTo requestData) throws BusinessException {

    }
}
