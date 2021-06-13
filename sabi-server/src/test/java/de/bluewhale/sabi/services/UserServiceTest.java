/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.*;
import de.bluewhale.sabi.model.NewRegistrationTO;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserProfileTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Locale;

import static de.bluewhale.sabi.TestDataFactory.*;
import static org.junit.Assert.*;


/**
 * Business-Layer tests for UserServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserServiceTest {
// ------------------------------ FIELDS ------------------------------

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

// -------------------------- OTHER METHODS --------------------------

/*
    @BeforeClass
    public static void init() throws NamingException {
    }
*/

/*    @AfterClass
    public static void tearDownClass() throws Exception {
    }
*/


    /**
     * This test was written to demonstrate that @NotNull Annotations won't do anything if confronted with null values.
     * The reason for this is, that this is only half of the part of correct bean validation.
     * As soon as we start to introduce constraint checking through a JSR-303 Validator class, this test might fail.
     *
     * @throws Exception
     */
    @Test
    public void testNullValidation() throws Exception {
        // Given
        // Nothing
        // When
        userService.validateUser(null, null);
        // Then
        // No Exception occurs
    }


    @Test
    @Transactional
    public void testRegisterExistingUserWithSameEmailAdress() throws Exception {
        // Given
        NewRegistrationTO userTo1 = new NewRegistrationTO(TESTUSER_EMAIL1, "User1", VALID_PASSWORD);
        final ResultTo<UserTo> firstUserResultTo = userService.registerNewUser(userTo1);

        // When
        NewRegistrationTO userTo2 = new NewRegistrationTO(TESTUSER_EMAIL1, "User2", VALID_PASSWORD);
        final ResultTo<UserTo> userResultTo = userService.registerNewUser(userTo2);

        // Then
        assertNotNull(userResultTo);
        assertNull(userResultTo.getValue());
        final Message message = userResultTo.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                AuthExceptionCodes.USER_REGISTRATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_EMAIL);
    }

    @Test
    @Transactional
    public void testUserProfileUpdate() throws Exception {
        // Given
        NewRegistrationTO userTo1 = new NewRegistrationTO(TESTUSER_EMAIL1, "User1", VALID_PASSWORD);
        userTo1.setLanguage(Locale.GERMAN.getLanguage());
        userTo1.setCountry(Locale.GERMAN.getCountry());
        final ResultTo<UserTo> firstUserResultTo = userService.registerNewUser(userTo1);

        // When
        UserProfileTo userProfileTo = new UserProfileTo(
                Locale.ENGLISH.getLanguage(),
                Locale.ENGLISH.getCountry());

        ResultTo<UserProfileTo> userProfileResultTo = userService.updateProfile(userProfileTo, TESTUSER_EMAIL1);
        UserEntity userEntity = userRepository.getByEmail(TESTUSER_EMAIL1);

        // Then
        final Message message = userProfileResultTo.getMessage();
        assertNotNull(message);
        assertEquals(message.getCode(), CommonMessageCodes.UPDATE_SUCCEEDED);

        assertEquals(userEntity.getLanguage(), Locale.ENGLISH.getLanguage());
        assertEquals(userEntity.getCountry(), Locale.ENGLISH.getCountry());
    }

    @Test
    @Transactional
    public void testGetUserProfile() throws Exception {
        // Given
        NewRegistrationTO userTo1 = new NewRegistrationTO(TESTUSER_EMAIL1, "User1", VALID_PASSWORD);
        userTo1.setLanguage(Locale.GERMAN.getLanguage());
        userTo1.setCountry(Locale.GERMAN.getCountry());
        final ResultTo<UserTo> firstUserResultTo = userService.registerNewUser(userTo1);

        // When
        ResultTo<UserProfileTo> userProfileToResultTo = userService.getUserProfile(TESTUSER_EMAIL1);

        // Then
        final Message message = userProfileToResultTo.getMessage();
        assertNotNull(userProfileToResultTo);
        assertNotNull(message);
        assertEquals(message.getCode(), CommonMessageCodes.OK);

        UserProfileTo userProfileTo = userProfileToResultTo.getValue();
        assertNotNull(userProfileTo);
        assertEquals(userProfileTo.getLanguage(),userTo1.getLanguage());
        assertEquals(userProfileTo.getCountry(),userTo1.getCountry());

    }

    @Test(expected = BusinessException.class)
    @Transactional
    public void testFraudUserProfileUpdate() throws Exception {
        // Given
        NewRegistrationTO userTo1 = new NewRegistrationTO(TESTUSER_EMAIL1, "User1", VALID_PASSWORD);
        userTo1.setLanguage(Locale.GERMAN.getLanguage());
        userTo1.setCountry(Locale.GERMAN.getCountry());
        final ResultTo<UserTo> firstUserResultTo = userService.registerNewUser(userTo1);

        // When
        UserProfileTo userProfileTo = new UserProfileTo(
                Locale.ENGLISH.getLanguage(),
                Locale.ENGLISH.getCountry());

        ResultTo<UserProfileTo> userProfileResultTo = userService.updateProfile(userProfileTo, "DOES@NOT.MATCH");

        // Then
        // expected Business Exception
    }

    @Test
    @Transactional
    public void testRegisterExistingUserWithSameUsername() throws Exception {
        // Given
        NewRegistrationTO userTo1 = new NewRegistrationTO(TESTUSER_EMAIL1, "User1", VALID_PASSWORD);
        final ResultTo<UserTo> firstUserResultTo = userService.registerNewUser(userTo1);

        // When
        NewRegistrationTO userTo2 = new NewRegistrationTO(TESTUSER_EMAIL2, "User1", VALID_PASSWORD);
        final ResultTo<UserTo> userResultTo = userService.registerNewUser(userTo2);

        // Then
        assertNotNull(userResultTo);
        assertNull(userResultTo.getValue());
        final Message message = userResultTo.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                AuthExceptionCodes.USER_REGISTRATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.USER_ALREADY_EXISTS_WITH_THIS_USERNAME);
    }

    @Test
    @Transactional
    public void testRegisterUser() throws Exception {
        // Given
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", VALID_PASSWORD);

        // When
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);

        // Then
        assertNotNull(userToResultTo);
        assertNotNull(userToResultTo.getValue());
        assertNotNull("User did not got an Id!", userToResultTo.getValue().getId());
        assertNotNull("New user was not issued with a token.", userToResultTo.getValue().getValidationToken());
    }


    /**
     * During register process the user gets an email with an token. This test the service to validate the token for the user.
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testUserValidation() throws Exception {
        // Given
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", VALID_PASSWORD);
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        final String token = userToResultTo.getValue().getValidationToken();

        // When
        boolean isValidated = userService.validateUser(userTo.getEmail(), token);
        boolean brokenValidation = userService.validateUser(userTo.getEmail(), "otherToken");

        // Then
        assertTrue(isValidated);
        assertFalse(brokenValidation);
    }


    /**
     * For users safety we are using a one way password encryption. So that the DB admin is not ensnared to misuse the data.
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testStoreEncryptedPasswords() throws Exception {
        // Given

        final String clearTextPassword = VALID_PASSWORD;
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", clearTextPassword);

        // When
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);

        // Then
        final UserTo storedUser = userToResultTo.getValue();
        final String encryptedPassword = storedUser.getPassword();
        assertNotEquals("Stored passwords must be encrypted!", clearTextPassword, encryptedPassword);
    }


    @Test
    @Transactional
    public void testSignIn() throws Exception {
        // Given
        final String clearTextPassword = VALID_PASSWORD;
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", clearTextPassword);
        ResultTo<UserTo> resultTo = userService.registerNewUser(userTo);
        userService.validateUser(TESTUSER_EMAIL1, resultTo.getValue().getValidationToken());

        // When
        @NotNull ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL1, clearTextPassword);

        // Then
        assertNotNull("Contract Broken? Expected Session Token", signInResult.getValue());

        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(AuthMessageCodes.SIGNIN_SUCCEEDED, message.getCode());
    }


    @Test
    @Transactional
    public void testSignInWithUnknownUserName() throws Exception {
        // Given
        // Nothing

        // When
        @NotNull ResultTo<String> signInResult = userService.signIn("I Don't care@anything", "abc");

        // Then
        assertNotNull(signInResult);
        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                AuthExceptionCodes.AUTHENTICATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.UNKNOWN_USERNAME);
    }


    @Test
    @Transactional
    public void testSignInWithUnknownOrWrongPassword() throws Exception {
        // Given a user which registered and validated his email address.
        final String clearTextPassword = VALID_PASSWORD;
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", clearTextPassword);
        ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        UserTo createdUserTo = userToResultTo.getValue();
        userService.validateUser(TESTUSER_EMAIL1, createdUserTo.getValidationToken());


        // When
        @NotNull ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL1, "abc");

        // Then
        assertNotNull(signInResult);
        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                AuthExceptionCodes.AUTHENTICATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.WRONG_PASSWORD);
    }


    @Test
    @Transactional
    public void testSignInWithUnvalidatedEmailFails() throws Exception {

        // For being able to login we need a clearly validated user account,
        // for which we use the verified email address

        // Given
        final String clearTextPassword = VALID_PASSWORD;
        NewRegistrationTO userTo = new NewRegistrationTO(TESTUSER_EMAIL1, "Tester", clearTextPassword);
        userService.registerNewUser(userTo);

        // When
        @NotNull ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL1, "abc");

        // Then
        assertNotNull(signInResult);
        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                AuthExceptionCodes.AUTHENTICATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.INCOMPLETE_REGISTRATION_PROCESS);
    }



}
