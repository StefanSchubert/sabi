/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.AuthExceptionCodes;
import de.bluewhale.sabi.exception.AuthMessageCodes;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.CommonExceptionCodes;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.*;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 * Provides all logic for user management use cases.
 */
public interface UserService {

    /**
     * Creates a new User. The returned object contains a result message along with the created
     * user (in success case). The user has been created successfully
     * only if the message is of {@link CATEGORY#INFO}
     * <b>Precondition:</b> The users email and username must both be unique.
     */
    @NotNull
    @Transactional
    ResultTo<UserTo> registerNewUser(@NotNull NewRegistrationTO newUser);

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
     * @param pEmailOrUsername   users login. can be email adress or username
     * @param pClearTextPassword his password
     * @return ResultTo with Authentication result. You need to check the message within the resultTo.
     *         In addition the result-To contains the users email address on success.
     * The Message must contain the following message code {@link AuthMessageCodes#SIGNIN_SUCCEEDED} otherwise the login failed.
     */
    @NotNull
    ResultTo<String> signIn(@NotNull String pEmailOrUsername, @NotNull String pClearTextPassword);

    /**
     * Will check if user email is registered and sends user an email with a onetime pass to reset his or hers password
     *
     * @param requestData Contains Emailaddress and Captcha Token
     * @throws BusinessException with {@link AuthExceptionCodes#USER_LOCKED} in case if user is not accessible (locked or unknown email),
     * {@link AuthExceptionCodes#AUTHENTICATION_FAILED} in case of an invalid request (Captcha failed) or
     * {@link AuthExceptionCodes#SERVICE_UNAVAILABLE} in case of a communication problem. User may retry later.
     * {@link AuthExceptionCodes#USER_LOCKED} in case email address is unknown or does not exists.
     *
     */
    void requestPasswordReset(@NotNull RequestNewPasswordTo requestData) throws BusinessException;

    /**
     * Checks if the user has submitted the correct token {@link ResetPasswordTo} and reset his password.
     * The token must match the one as provided through {@link UserService#requestPasswordReset(RequestNewPasswordTo)} before.
     * In case of troubles, remember that the token has a short time-to-live to protect against brute force attacks.
     *
     * @param requestData contains Email-Adress, New-Password and ResetToken.
     * @throws BusinessException with
     * {@link AuthExceptionCodes#PW_RESET_FAILED} in case of an invalid request (email, token or password) problem or
     * {@link AuthExceptionCodes#SERVICE_UNAVAILABLE} in case of a communication problem - you may retry later.
     * {@link AuthExceptionCodes#PASSWORD_TOO_WEAK} in case of a communication problem - you may retry later.
     */
    @Transactional
    void resetPassword(@NotNull ResetPasswordTo requestData) throws BusinessException;

    /**
     * Used to display some project stats.
     * @return Number of Participants.
     */
    String fetchAmountOfParticipants();

    /**
     * Use this whenever you require to update a users profile data.
     * @param userProfileTo user profile Data which will be updated
     * @param principalName provided by Spring Security and currently equal to users email address.
     *                      only when this emailaddress belongs the {@link UserProfileTo#userId} the update will be performed.
     * @return resultTo containing the successful updated data.
     * @throws BusinessException with
      {@link AuthExceptionCodes#SERVICE_UNAVAILABLE} in case of a communication problem - you may retry later.
      {@link AuthExceptionCodes#AUTHENTICATION_FAILED} in case of a missmatched principal
      {@link CommonExceptionCodes#} in case of insufficient data
     */
    @Transactional
    ResultTo<UserProfileTo> updateProfile(UserProfileTo userProfileTo, String principalName) throws BusinessException;

    /**
     * Use this to query a users profile data.
     * @param principalName provided by Spring Security and currently equal to users email address.
     * @return resultTo containing the user profile. Profile may by null in case of errors
     *         The Message in the resultTO must contain the following message code of type info
     *         {@link de.bluewhale.sabi.exception.CommonMessageCodes#OK} otherwise the request failed.
     * @throws BusinessException with
    {@link AuthExceptionCodes#SERVICE_UNAVAILABLE} in case of a communication problem - you may retry later.
    {@link AuthExceptionCodes#AUTHENTICATION_FAILED} in case of a missmatched principal
     */
    @NotNull ResultTo<UserProfileTo> getUserProfile(String principalName) throws BusinessException;
}
