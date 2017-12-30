/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import de.bluewhale.sabi.configs.HazelcastConfig;
import de.bluewhale.sabi.configs.HazelcastMapItem;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.RequestNewPasswordTo;
import de.bluewhale.sabi.model.ResetPasswordTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.validation.constraints.NotNull;
import java.util.Random;

import static de.bluewhale.sabi.util.Obfuscator.encryptPasswordForHeavensSake;

/**
 * User: Stefan Schubert
 * Date: 29.08.15
 */
@Service
public class UserServiceImpl extends CommonService implements UserService {

    @Autowired
    private UserDao dao;

    @Autowired
    private CaptchaAdapter captchaAdapter;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TokenAuthenticationService encryptionService;

    public ResultTo<UserTo> registerNewUser(@NotNull UserTo newUser) {

        String validateToken = generateValidationToken();
        newUser.setValidationToken(validateToken);
        newUser.setValidated(false);

        UserTo createdUser = null;
        Message message;
        try {
            String encryptedPassword = encryptPasswordForHeavensSake(newUser.getPassword());
            createdUser = dao.create(newUser, encryptedPassword);
            message = Message.info(AuthMessageCodes.USER_CREATION_SUCCEEDED, createdUser.getEmail());
        } catch (BusinessException pE) {
            message = Message.error(AuthMessageCodes.USER_ALREADY_EXISTS, newUser.getEmail());
        }

        final ResultTo<UserTo> userToResultTo = new ResultTo<>(createdUser, message);
        return userToResultTo;
    }


    private String generateValidationToken() {
        // Thanks goes to: Mister Smith (http://stackoverflow.com/questions/14622622/generating-a-random-hex-string-of-length-50-in-java-me-j2me)
        int numchars = 8;
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < numchars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, numchars);
    }

    @Override
    public void unregisterUserAndClearPersonalData(@NotNull String pEmail) {
        // Hook in here to delete other personal data
        dao.deleteByEmail(pEmail);
    }

    @Override
    public boolean validateUser(@NotNull final String pEmail, @NotNull final String pToken) {
        boolean result = false;
        if (pEmail != null && pToken != null) {
            final UserTo userTo = dao.loadUserByEmail(pEmail);
            if (pToken.equals(userTo.getValidationToken())) {
                try {
                    dao.toggleValidationFlag(pEmail, true);
                    result = true;
                } catch (BusinessException pE) {
                    result = false;
                }
            }
        }
        return result;
    }


    @Override
    public ResultTo<String> signIn(@NotNull final String pEmail, @NotNull final String pClearTextPassword) {
        // todo integrate some password policy here and throw an too weak exception
        final String password = encryptPasswordForHeavensSake(pClearTextPassword);
        final UserTo userTo = dao.loadUserByEmail(pEmail);
        if (userTo != null) {

            if (!userTo.isValidated()) {
                final Message errorMsg = Message.info(AuthMessageCodes.INCOMPLETE_REGISTRATION_PROCESS, pEmail);
                return new ResultTo<String>("Email address validation missing.", errorMsg);
            } else if (userTo.getPassword().equals(password)) {
                final Message successMessage = Message.info(AuthMessageCodes.SIGNIN_SUCCEEDED, pEmail);
                return new ResultTo<String>("Happy", successMessage);
            } else {
                final Message errorMsg = Message.error(AuthMessageCodes.WRONG_PASSWORD, pEmail);
                return new ResultTo<String>("Sad", errorMsg);
            }
        } else {
            final Message errorMsg = Message.error(AuthMessageCodes.UNKNOWN_USERNAME, pEmail);
            return new ResultTo<String>("Fraud?", errorMsg);
        }

    }

    @Override
    public void resetPassword(@NotNull ResetPasswordTo requestData) throws BusinessException {


        String emailAddress = requestData.getEmailAddress();
        String resetToken = requestData.getResetToken();
        String newPassword = requestData.getNewPassword();
        if ((emailAddress == null) ||
                (resetToken == null) ||
                (newPassword == null)) {
            throw BusinessException.with(AuthMessageCodes.INCONSISTENT_PW_RESET_DATA);
        }

        // todo integrate pw-policy and throw an Password_Too_Weak

        HazelcastInstance hzInstance = Hazelcast.getHazelcastInstanceByName(HazelcastConfig.HZ_INSTANCE_NAME);

        // store token in distributed cache
        IMap<String, String> cachedTokenMap = hzInstance.getMap(HazelcastMapItem.PASSWORD_FORGOTTEN_TOKEN);

        String cachedToken = cachedTokenMap.get(emailAddress);
        if (cachedToken != null && resetToken.equals(cachedToken)) {
            String encryptedPassword = encryptPasswordForHeavensSake(newPassword);
            dao.resetPassword(emailAddress, encryptedPassword);

            try {
                notificationService.sendPasswordResetConfirmation(emailAddress);
            } catch (MessagingException e) {
                e.printStackTrace();
            }

        } else {
            throw BusinessException.with(AuthMessageCodes.UNKNOWN_OR_STALE_PW_RESET_TOKEN);
        }

    }

    @Override
    public void requestPasswordReset(RequestNewPasswordTo requestData) throws BusinessException {

        String captchaToken = requestData.getCaptchaToken();
        Boolean captchaValid = captchaAdapter.isCaptchaValid(captchaToken);

        HazelcastInstance hzInstance = Hazelcast.getHazelcastInstanceByName(HazelcastConfig.HZ_INSTANCE_NAME);

        if (captchaValid == null) {
            throw new BusinessException(AuthExceptionCodes.SERVICE_UNAVAILABLE, Message.error(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE));
        }

        if (captchaValid == false) {
            throw new BusinessException(AuthExceptionCodes.AUTHENTICATION_FAILED, Message.error(AuthMessageCodes.CORRUPTED_TOKEN_DETECTED));
        } else {

            String emailAddress = requestData.getEmailAddress();
            try {
                new InternetAddress(emailAddress);
            } catch (AddressException e) {
                throw new BusinessException(AuthExceptionCodes.AUTHENTICATION_FAILED, Message.error(AuthMessageCodes.INVALID_EMAIL_ADDRESS));
            }

            if (emailAddress != null && dao.loadUserByEmail(emailAddress) != null) {
                final UserTo userTo = dao.loadUserByEmail(emailAddress);
                if (userTo != null) {
                    // We generate the token only for a user that really exists.
                    String token = generateValidationToken();

                    // store token in distributed cache
                    IMap<String, String> cachedTokenMap = hzInstance.getMap(HazelcastMapItem.PASSWORD_FORGOTTEN_TOKEN);
                    cachedTokenMap.put(emailAddress, token);

                    try {
                        notificationService.sendPasswordResetToken(emailAddress, token);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                throw new BusinessException(AuthExceptionCodes.USER_LOCKED, Message.error(AuthMessageCodes.EMAIL_NOT_REGISTERED));
            }
        }
    }

}
