/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
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
import de.bluewhale.sabi.util.TestDataFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;


/**
 * Business-Layer tests for UserServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserServiceTest {
    
        /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

	static TestDataFactory testDataFactory  = TestDataFactory.getInstance();

	@Container
	@ServiceConnection
	// This does the trick. Spring Autoconfigures itself to connect against this container data requests-
	static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);
	
// ------------------------------ FIELDS ------------------------------

	@Autowired
	UserService userService;

	@Autowired
	PasswordEncoder passwordEncoder;

	@MockBean
	UserRepository userRepository;

// -------------------------- OTHER METHODS --------------------------

	
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
	@Rollback
	public void testRegisterExistingUserWithSameEmailAddress() throws Exception {
		// Given

		NewRegistrationTO newRegistrationTO = testDataFactory.getNewRegistrationTO(TESTUSER_EMAIL1);

		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity allreadyExistingUser = testDataFactory.getNewTestUserEntity(testUserTo);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(allreadyExistingUser);
		given(userRepository.getByUsername(testUserTo.getUsername())).willReturn(allreadyExistingUser);

		// When
		final ResultTo<UserTo> userResultTo = userService.registerNewUser(newRegistrationTO);

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
	@Rollback
	public void testUserProfileUpdate() throws Exception {
		// Given
		UserProfileTo userProfileTo = testDataFactory.getBasicUserProfileTo();
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);

		// Mocking the UserRepository
		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(testUserEntity);

		// When
		ResultTo<UserProfileTo> userProfileResultTo = userService.updateProfile(userProfileTo, TESTUSER_EMAIL1);

		// Then
		final Message message = userProfileResultTo.getMessage();
		assertNotNull(message);
		assertEquals(message.getCode(), CommonMessageCodes.UPDATE_SUCCEEDED);
	}

	@Test
	public void testGetUserProfile() throws Exception {
		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(newTestUserEntity);

		// When
		ResultTo<UserProfileTo> userProfileToResultTo = userService.getUserProfile(TESTUSER_EMAIL1);

		// Then
		final Message message = userProfileToResultTo.getMessage();
		assertNotNull(userProfileToResultTo);
		assertNotNull(message);
		assertEquals(message.getCode(), CommonMessageCodes.OK);

		UserProfileTo userProfileTo = userProfileToResultTo.getValue();
		assertNotNull(userProfileTo);
	}

	@Test
	@Rollback
	public void testFraudUserProfileUpdate() throws Exception {
		assertThrows(BusinessException.class, () -> {

			// Given
			UserProfileTo basicUserProfileTo = testDataFactory.getBasicUserProfileTo();
			given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(null);

			// When (result in an exception)
			ResultTo<UserProfileTo> userProfileResultTo = userService.updateProfile(basicUserProfileTo, "DOES@NOT.MATCH");

		});
	}

	@Test
	@Rollback
	public void testRegisterExistingUserWithSameUsername() throws Exception {
		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		NewRegistrationTO newRegistrationTO = testDataFactory.getNewRegistrationTO(TESTUSER_EMAIL1);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(null);
		given(userRepository.getByUsername(any())).willReturn(newTestUserEntity);


		// When
		final ResultTo<UserTo> userResultTo = userService.registerNewUser(newRegistrationTO);

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
	@Rollback
	public void testRegisterUser() throws Exception {
		// Given
		NewRegistrationTO newRegistrationTO = testDataFactory.getNewRegistrationTO(TESTUSER_EMAIL1);
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		newTestUserEntity.setValidateToken("123456");


		// Mocking the UserRepository
		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(null);
		given(userRepository.getByUsername(any())).willReturn(null);
		given(userRepository.saveAndFlush(any())).willReturn(newTestUserEntity);

		// When
		final ResultTo<UserTo> userToResultTo = userService.registerNewUser(newRegistrationTO);

		// Then
		assertNotNull(userToResultTo);
		assertNotNull(userToResultTo.getValue());
		assertNotNull(userToResultTo.getValue().getId());
		assertNotNull(userToResultTo.getValue().getValidationToken());
	}


	/**
	 * During register process the user gets an email with an token. This test the service to validate the token for the user.
	 *
	 * @throws Exception
	 */
	@Test
	public void testUserValidation() throws Exception {
		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(newTestUserEntity);

		// When
		boolean isValidated = userService.validateUser(testUserTo.getEmail(), newTestUserEntity.getValidateToken());
		boolean brokenValidation = userService.validateUser(testUserTo.getEmail(), "otherToken");

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
	public void testStoreEncryptedPasswords() throws Exception {
		// Given
		NewRegistrationTO newRegistrationTO = testDataFactory.getNewRegistrationTO(TESTUSER_EMAIL1);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(null);
		given(userRepository.getByUsername(any())).willReturn(null);
		when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		final ResultTo<UserTo> userToResultTo = userService.registerNewUser(newRegistrationTO);

		// Then
		final UserTo storedUser = userToResultTo.getValue();
		final String encryptedPassword = storedUser.getPassword();
		assertNotEquals("Stored passwords must be encrypted!", newRegistrationTO.getPassword(), encryptedPassword);
	}


	@Test
	public void testSignIn() throws Exception {
		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		newTestUserEntity.setPassword(passwordEncoder.encode(testUserTo.getPassword()));

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(newTestUserEntity);

		// When
		@NotNull ResultTo<String> signInResult = userService.signIn(TESTUSER_EMAIL1, testUserTo.getPassword());

		// Then
		assertNotNull("Contract Broken? Expected Session Token", signInResult.getValue());

		final Message message = signInResult.getMessage();
		assertNotNull(message);
		assertNotNull(message.getCode());
		assertEquals(AuthMessageCodes.SIGNIN_SUCCEEDED, message.getCode());
	}


	@Test
	public void testSignInWithUnknownUserName() throws Exception {
		// Given
		given(userRepository.getByEmail(any())).willReturn(null);
		given(userRepository.getByUsername(any())).willReturn(null);

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
	public void testSignInWithUnknownOrWrongPassword() throws Exception {
		// Given a user which registered and validated his email address.
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(newTestUserEntity);

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
	public void testSignInWithUnvalidatedEmailFails() throws Exception {

		// For being able to login we need a clearly validated user account,
		// for which we use the verified email address

		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity newTestUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		newTestUserEntity.setValidated(false);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(newTestUserEntity);

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
