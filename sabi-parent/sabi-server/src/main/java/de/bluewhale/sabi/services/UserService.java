package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;

import javax.validation.constraints.NotNull;

/**
 * Provides all required services for user management use cases.
 */
public interface UserService {

    /**
     * Creates a new User. The returned object contains the created
     * user with a message. The user has been created successfully
     * only if the message is of {@link CATEGORY#INFO}
     */
    @NotNull ResultTo<UserTo> registerNewUser(@NotNull UserTo newUser);


    /**
     * Drops the user and all of his not commonly shared data.
     * @param pEmail unique business identifier of the user
     */
    void unregisterUserAndClearPersonalData(@NotNull String pEmail);


    /**
     * User received a validation mail and clicks on the link, to validate his email address. This is the method which does the check.
     * @param pEmail
     * @param pToken
     * @return validation status, false if validation failed, true otherwise
     */
    boolean validateUser(@NotNull String pEmail, @NotNull String pToken);


    /**
     * Use this to signin the user
     * @param pEmail his email address which is being used to login
     * @param pClearTextPassword his password
     * @return ResultTo with an AccessToken as value which may be null and a result message.
     */
    @NotNull ResultTo<String> signIn(@NotNull String pEmail, @NotNull String pClearTextPassword);
}
