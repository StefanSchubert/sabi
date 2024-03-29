/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import de.bluewhale.sabi.configs.HazelcastConfig;
import de.bluewhale.sabi.configs.HazelcastMapItem;
import de.bluewhale.sabi.exception.*;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.UserMeasurementReminderEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.PasswordPolicy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;


/**
 * User: Stefan Schubert
 * Date: 29.08.15
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaptchaAdapter captchaAdapter;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    public ResultTo<UserTo> registerNewUser(@NotNull NewRegistrationTO pRegistrationUserTo) {

        // mapping incoming reqTo to internal UserTo to suite service contract
        UserTo newUserTO = new UserTo(pRegistrationUserTo.getEmail(), pRegistrationUserTo.getUsername(), pRegistrationUserTo.getPassword(),
                pRegistrationUserTo.getLanguage(), pRegistrationUserTo.getCountry());

        String validateToken = generateValidationToken();
        newUserTO.setValidationToken(validateToken);
        newUserTO.setValidated(false);

        UserTo createdUser = null;
        Message message;

        UserEntity alreadyExistingUserWithSameEmail = userRepository.getByEmail(newUserTO.getEmail());
        UserEntity alreadyExistingUserWithSameUsername = userRepository.getByUsername(newUserTO.getUsername());
        if ((alreadyExistingUserWithSameEmail == null) && (alreadyExistingUserWithSameUsername == null)) {

            UserEntity newUserEntity = userMapper.mapUserTo2Entity(newUserTO);
            newUserTO.setId(null); // to make sure we have no collision

            if (!PasswordPolicy.isPasswordValid(newUserTO.getPassword())) {
                message = Message.error(AuthMessageCodes.PASSWORD_TO_WEAK, "At least 10 Character, Digit, Special Sign, Capitalletter required");
            } else {

                newUserEntity.setPassword(passwordEncoder.encode(newUserTO.getPassword()));
                newUserEntity.setValidated(false);

                UserEntity userEntity = userRepository.saveAndFlush(newUserEntity);
                createdUser = userMapper.mapUserEntity2To(userEntity);

                message = Message.info(AuthMessageCodes.USER_CREATION_SUCCEEDED, createdUser.getEmail());
            }
        } else {
            if (alreadyExistingUserWithSameEmail != null) {
                message = Message.error(AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_EMAIL, newUserTO.getEmail());
            } else {
                message = Message.error(AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_USERNAME, newUserTO.getUsername());
            }
        }

        final ResultTo<UserTo> userToResultTo = new ResultTo<>(createdUser, message);
        return userToResultTo;
    }

    @Override
    public ResultTo<UserProfileTo> getUserProfile(String principalName) throws BusinessException {

        UserProfileTo userProfileTo = null;
        UserEntity userEntity;

        try {
            userEntity = userRepository.getByEmail(principalName);
        } catch (Exception e) {
            throw BusinessException.with(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE);
        }

        if (userEntity == null ) {
            throw BusinessException.with(AuthMessageCodes.INVALID_EMAIL_ADDRESS);
        }

        Message info = Message.info(CommonMessageCodes.OK);
        userProfileTo = new UserProfileTo(userEntity.getLanguage(),userEntity.getCountry());
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        return userProfileResultTo;
    }

    @Override
    public ResultTo<UserProfileTo> updateProfile(UserProfileTo userProfileTo, String principalName) throws BusinessException {

        UserEntity existingUser;

        if (incompleteUserProfile(userProfileTo)) {
            log.error("Tried to update {} with incomplete profile data {}", principalName, userProfileTo);
            throw BusinessException.with(CommonMessageCodes.INSUFFICIENT_DATA);
        }

        try {
            existingUser = userRepository.getByEmail(principalName);
        } catch (Exception e) {
            throw BusinessException.with(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE);
        }
        if (existingUser != null) {
            existingUser.setLanguage(userProfileTo.getLanguage());
            existingUser.setCountry(userProfileTo.getCountry());

            List<UserMeasurementReminderEntity> userMeasurementReminders = existingUser.getUserMeasurementReminders();
            List<MeasurementReminderTo> updateReminderToList = userProfileTo.getMeasurementReminderTos();
            for (UserMeasurementReminderEntity userMeasurementReminder : userMeasurementReminders) {
                Optional<MeasurementReminderTo> optionalMeasurementReminderTo = updateReminderToList.stream().filter(item -> item.getUnitId() == userMeasurementReminder.getUnitId()).findFirst();
                if (optionalMeasurementReminderTo.isPresent()) {
                    userMeasurementReminder.setPastdays(optionalMeasurementReminderTo.get().getPastDays());
                    userMeasurementReminder.setActive(optionalMeasurementReminderTo.get().isActive());
                }
            }

        } else {
            throw BusinessException.with(AuthMessageCodes.INVALID_EMAIL_ADDRESS);
        }

        Message info = Message.info(CommonMessageCodes.UPDATE_SUCCEEDED);
        final ResultTo<UserProfileTo> userProfileResultTo = new ResultTo<>(userProfileTo, info);
        return userProfileResultTo;
    }

    private boolean incompleteUserProfile(UserProfileTo userProfileTo) {
        boolean result = false;
        if (userProfileTo == null ||
                userProfileTo.getCountry() == null ||
                userProfileTo.getLanguage() == null) {
            result = true;
        }
        return result;
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
        UserEntity userEntity = userRepository.getByEmail(pEmail);
        if (userEntity != null) {
            userRepository.delete(userEntity);
        }
    }

    @Override
    public boolean validateUser(@NotNull final String pEmail, @NotNull final String pToken) {
        boolean result = false;
        if (pEmail != null && pToken != null) {
            UserEntity userEntity = userRepository.getByEmail(pEmail);
            if ((userEntity != null) && pToken.equals(userEntity.getValidateToken())) {
                userEntity.setValidated(true); // save will be transformed by transaction close
                result = true;
            }
        }
        return result;
    }


    @Override
    public ResultTo<String> signIn(@NotNull final String pEmailOrUsername, @NotNull final String pClearTextPassword) {

        UserEntity userEntity;

        // we allow to login with email or username
        userEntity = userRepository.getByEmail(pEmailOrUsername);
        if (userEntity == null) {
            userEntity = userRepository.getByUsername(pEmailOrUsername);
        }

        if (userEntity != null) {

            if (!userEntity.isValidated()) {
                final Message errorMsg = Message.info(AuthMessageCodes.INCOMPLETE_REGISTRATION_PROCESS, pEmailOrUsername);
                return new ResultTo<String>("Mailvalidation not finished yet.", errorMsg);
            } else if (passwordEncoder.matches(pClearTextPassword, userEntity.getPassword())) {
                final Message successMessage = Message.info(AuthMessageCodes.SIGNIN_SUCCEEDED, pEmailOrUsername);
                return new ResultTo<String>(userEntity.getEmail(), successMessage);
            } else {
                final Message errorMsg = Message.error(AuthMessageCodes.WRONG_PASSWORD, pEmailOrUsername);
                return new ResultTo<String>("Sorry - no way.", errorMsg);
            }
        } else {
            final Message errorMsg = Message.error(AuthMessageCodes.UNKNOWN_USERNAME, pEmailOrUsername);
            return new ResultTo<String>("Sorry - Unknown Account", errorMsg);
        }
    }

    @Override
    public String fetchAmountOfParticipants() {
        return String.valueOf(userRepository.count());
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

        if (!PasswordPolicy.isPasswordValid(newPassword)) {
            throw BusinessException.with(AuthMessageCodes.PASSWORD_TO_WEAK);
        }

        // todo integrate pw-policy and throw an Password_Too_Weak

        HazelcastInstance hzInstance = Hazelcast.getHazelcastInstanceByName(HazelcastConfig.HZ_INSTANCE_NAME);

        // store token in distributed cache
        IMap<String, String> cachedTokenMap = hzInstance.getMap(HazelcastMapItem.PASSWORD_FORGOTTEN_TOKEN);

        String cachedToken = cachedTokenMap.get(emailAddress);
        if (cachedToken != null && resetToken.equals(cachedToken)) {

            UserEntity userEntity = userRepository.getByEmail(emailAddress);
            if (userEntity != null) {
                userEntity.setPassword(passwordEncoder.encode(newPassword));
                try {
                    notificationService.sendPasswordResetConfirmation(emailAddress);
                } catch (MessagingException e) {
                    e.printStackTrace();
                    // todo refactor to reflect an internal error
                }
            }

        } else {
            throw BusinessException.with(AuthMessageCodes.UNKNOWN_OR_STALE_PW_RESET_TOKEN);
        }
    }

    @Override
    public void requestPasswordReset(RequestNewPasswordTo requestData) throws BusinessException {

        String captchaToken = requestData.getCaptchaToken();
        Boolean captchaValid = null;
        try {
            captchaValid = captchaAdapter.isCaptchaValid(captchaToken);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(AuthExceptionCodes.SERVICE_UNAVAILABLE, Message.error(AuthMessageCodes.BACKEND_TEMPORARILY_UNAVAILABLE));
        }

        HazelcastInstance hzInstance = Hazelcast.getHazelcastInstanceByName(HazelcastConfig.HZ_INSTANCE_NAME);

        if (!captchaValid) {
            throw new BusinessException(AuthExceptionCodes.AUTHENTICATION_FAILED, Message.error(AuthMessageCodes.CORRUPTED_TOKEN_DETECTED));
        } else {

            String emailAddress = requestData.getEmailAddress();
            try {
                new InternetAddress(emailAddress);
            } catch (AddressException e) {
                throw new BusinessException(AuthExceptionCodes.AUTHENTICATION_FAILED, Message.error(AuthMessageCodes.INVALID_EMAIL_ADDRESS));
            }

            if (emailAddress != null && userRepository.existsUserEntityByEmailEquals(emailAddress)) {
                UserEntity userEntity = userRepository.getByEmail(emailAddress);

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
            } else {
                throw new BusinessException(AuthExceptionCodes.USER_LOCKED, Message.error(AuthMessageCodes.EMAIL_NOT_REGISTERED));
            }
        }
    }

}
