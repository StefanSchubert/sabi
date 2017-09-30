/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 * Provides all required services for user management use cases.
 */
public interface UserService {

    /**
     * Creates a new User. The returned object contains a result message along with the created
     * user (in success case). The user has been created successfully
     * only if the message is of {@link CATEGORY#INFO}
     */
    @NotNull
    @Transactional
    ResultTo<UserTo> registerNewUser(@NotNull UserTo newUser);

    /**
     * Drops the user and all of his not commonly shared data.
     *
     * @param pEmail unique business identifier of the user
     */
    @Transactional
    void unregisterUserAndClearPersonalData(@NotNull String pEmail);

    /**
     * User received a validation mail and clicks on the link, to validate his email address. This is the method which does the check.
     *
     * @param pEmail
     * @param pToken
     * @return validation status, false if validation failed, true otherwise
     */
    @Transactional
    boolean validateUser(@NotNull String pEmail, @NotNull String pToken);

    /**
     * Use this to signin the user
     *
     * @param pEmail             his email address which is being used to login
     * @param pClearTextPassword his password
     * @return ResultTo with Authentication result. You need to check the message within the resultTo.
     * The Message must contain the following message code {@link AuthMessageCodes#SIGNIN_SUCCEEDED} otherwise the login failed.
     */
    @NotNull
    ResultTo<String> signIn(@NotNull String pEmail, @NotNull String pClearTextPassword);


}
