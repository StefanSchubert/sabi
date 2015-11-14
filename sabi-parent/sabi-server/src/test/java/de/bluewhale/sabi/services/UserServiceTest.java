package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;


/**
 * Business-Layer tests for UserServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class UserServiceTest {
// ------------------------------ FIELDS ------------------------------

    private static final String TESTUSER_EMAIL = "testservice@bluewhale.de";

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

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
    public void testRegisterExistingUser() throws Exception {
        // Given
        UserTo userTo1 = new UserTo(TESTUSER_EMAIL, "NoPass123");
        final ResultTo<UserTo> firstuserResultTo = userService.registerNewUser(userTo1);

        // When
        UserTo userTo2 = new UserTo(TESTUSER_EMAIL, "NoPass123");
        final ResultTo<UserTo> userResultTo = userService.registerNewUser(userTo2);

        // Then
        assertNotNull(userResultTo);
        assertNull(userResultTo.getValue());
        final Message message = userResultTo.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                     AuthExceptionCodes.USER_REGISTRATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.USER_ALREADY_EXISTS);
    }


    @Test
    @Transactional
    public void testRegisterUser() throws Exception {
        // Given
        UserTo userTo = new UserTo(TESTUSER_EMAIL, "NoPass123");

        // When
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);

        // Then
        assertNotNull(userToResultTo);
        assertNotNull(userToResultTo.getValue());
        assertNotNull("New user was not issued with a token.", userToResultTo.getValue().getValidateToken());
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
        UserTo userTo = new UserTo(TESTUSER_EMAIL, "NoPass123");
        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        final String token = userToResultTo.getValue().getValidateToken();

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

        final String clearTextPassword = "NoPass123";
        UserTo userTo = new UserTo(TESTUSER_EMAIL, clearTextPassword);

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
        final String clearTextPassword = "NoPass123";
        UserTo userTo = new UserTo(TESTUSER_EMAIL, clearTextPassword);
        userService.registerNewUser(userTo);

        // When
        ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL, clearTextPassword);

        // Then
        assertNotNull("Contract Broken? Expected Session Token", signInResult.getValue());

        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode(), AuthMessageCodes.SIGNIN_SUCCEDED);
    }


    @Test
    @Transactional
    public void testSignInWithUnknownUserName() throws Exception {
        // Given
        // Nothing

        // When
        ResultTo<String> signInResult = userService.signIn("I Don't care@anything", "abc");

        // Then
        assertNotNull(signInResult);
        assertNull(signInResult.getValue());
        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                     AuthExceptionCodes.AUTHENTICATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.UNKNOWN_USERNAME);
    }


    @Test
    @Transactional
    public void testSignInWithUnknownPassword() throws Exception {
        // Given
        final String clearTextPassword = "NoPass123";
        UserTo userTo = new UserTo(TESTUSER_EMAIL, clearTextPassword);
        userService.registerNewUser(userTo);

        // When
        ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL, "abc");

        // Then
        assertNotNull(signInResult);
        assertNull(signInResult.getValue());
        final Message message = signInResult.getMessage();
        assertNotNull(message);
        assertNotNull(message.getCode());
        assertEquals(message.getCode().getExceptionCode(),
                     AuthExceptionCodes.AUTHENTICATION_FAILED);
        assertEquals(message.getCode(), AuthMessageCodes.WRONG_PASSWORD);
    }


    /**
     * After signin the user is issued a token, which he is required to pass with each request to get the user context through the REST APIs.
     * The token is supposed to be valid until his TTL has expired. If so the user need to relogin to refresh his token.
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testCheckValidToken() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


}
