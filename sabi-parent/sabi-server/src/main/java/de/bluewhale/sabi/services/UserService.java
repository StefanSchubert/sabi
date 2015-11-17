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
    @NotNull
    ResultTo<UserTo> registerNewUser(@NotNull UserTo newUser);

    /**
     * Drops the user and all of his not commonly shared data.
     *
     * @param pEmail unique business identifier of the user
     */
    void unregisterUserAndClearPersonalData(@NotNull String pEmail);

    /**
     * User received a validation mail and clicks on the link, to validate his email address. This is the method which does the check.
     *
     * @param pEmail
     * @param pToken
     * @return validation status, false if validation failed, true otherwise
     */
    boolean validateUser(@NotNull String pEmail, @NotNull String pToken);

    /**
     * Use this to signin the user
     *
     * @param pEmail             his email address which is being used to login
     * @param pClearTextPassword his password
     * @return ResultTo with an AccessToken as value which may be null and a result message.
     */
    @NotNull
    ResultTo<String> signIn(@NotNull String pEmail, @NotNull String pClearTextPassword);

    /**
     * Checks if the token is still valid.
     *
     * @param accessToken
     * @return false if token could not be decoded or has an expired TTL, true otherwise.
     */
    boolean isTokenValid(@NotNull String accessToken);

    /**
     * Extended check of the provided token.
     *
     * @param accessToken
     * @return resultTo with the decoded userIdentifier as value and one of those messages
     *         {@link AuthMessageCodes#TOKEN_VALID}, {@link AuthMessageCodes#TOKEN_EXPIRED} telling
     *         if the token is valid or not.
     *         If the token could not be decoded, the value will be null and a message {@link AuthMessageCodes#CORRUPTED_TOKEN_DETECTED}
     */
    ResultTo<String> checkToken(@NotNull String accessToken);
}
