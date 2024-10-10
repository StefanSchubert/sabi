/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.mapper.UserMapper;
import de.bluewhale.sabi.model.AccountCredentialsTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.services.CaptchaAdapter;
import de.bluewhale.sabi.util.RestHelper;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.naming.NamingException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;


/**
 * Checks user authorization workflows from client point of view.
 * You may consult the test cases, while developing a specific client.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("ModuleTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserAuthControllerTest extends CommonTestController {

	SimpleSmtpServer smtpServer;

	@Autowired
	ObjectMapper objectMapper;  // json mapper

	@MockBean
	CaptchaAdapter captchaAdapter;

	@MockBean
	UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserMapper userMapper;

	@BeforeEach
	public void initFakeMailer() throws NamingException {
		smtpServer = SimpleSmtpServer.start(2525);
	}


	@AfterEach
	public void stopFakeMailer() throws Exception {
		smtpServer.stop();
	}

	@AfterEach
	public void cleanUpMocks() {
		reset(captchaAdapter, userRepository);
	}


	/**
	 * Invalid captcha code results in 412
	 *
	 * @throws Exception
	 */
	@Test // REST-API
	public void testNewUserRegistrationWithInvalidCaptcha() throws Exception {

		// given a test user
		UserTo newUser = new UserTo("test@bluewhale.de", "Tester", "test123");
		newUser.setCaptchaCode("captcha mock not programmed so check will be false.");

		// when a new user sends a sign-in request
		HttpHeaders httpHeader = RestHelper.buildHttpHeader();
		String requestJson = objectMapper.writeValueAsString(newUser);

		try {
			ResponseEntity<String> responseEntity = restClient.post()
					.uri(Endpoint.REGISTER.getPath())
					.headers(headers -> headers.addAll(httpHeader))  // Set headers
					.body(requestJson)  // Set the request body
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(String.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Precondition_Failed to be thrown");

		} catch (HttpClientErrorException e) {
			// then we should get a 412 as result.
			assertThat("Faked access token should produce an unauthorized status.", e.getStatusCode().equals(HttpStatus.PRECONDITION_FAILED));
		}
	}


	/**
	 * Test registration process with a mocked captcha and dao service.
	 *
	 * @throws Exception
	 */
	@Test // REST-API
	// @Transactional // Usually takes care of rollback but does not work here
	// transaction is not being spanned over the restTemplate call.
	// so we mock the database as well here.
	public void testSuccessfulNewUserRegistration() throws Exception {

		// given a test user
		UserTo userTo = new UserTo("test@bluewhale.de", "Tester", TestDataFactory.VALID_PASSWORD);
		userTo.setCaptchaCode("test");
		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		// given a mocked captcha service - accepting our captcha
		given(this.captchaAdapter.isCaptchaValid(userTo.getCaptchaCode())).willReturn(Boolean.TRUE);
		// given mocked database backend
		given(this.userRepository.saveAndFlush(any(UserEntity.class))).willReturn(userEntity);

		// when a new user sends a sign-in request
		HttpHeaders httpHeaders = RestHelper.buildHttpHeader();
		String requestJson = objectMapper.writeValueAsString(userTo);

		ResponseEntity<UserTo> responseEntity = restClient.post()
				.uri(Endpoint.REGISTER.getPath())
				.headers(headers -> headers.addAll(httpHeaders))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 201, (request, response) -> {
					// then we should get a 201 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(UserTo.class);  // Converts the response to a ResponseEntity

		// and we should get an account validation mail must have been sent
		SmtpMessage smtpMessage = ((SmtpMessage) smtpServer.getReceivedEmail().next());
		assertThat(smtpMessage.getHeaderValue("Subject"), containsString("sabi Account Validation"));
	}

	/**
	 * Test registration process with a mocked captcha and dao service.
	 *
	 * @throws Exception
	 */
	@Test // REST-API
	// @Transactional // Usually takes care of rollback but does not work here
	// transaction is not being spanned over the restTemplate call.
	// so we mock the database as well here.
	public void testWeakPasswordUserRegistration() throws Exception {

		// given a test user
		UserTo userTo = new UserTo("test@bluewhale.de", "Tester", TestDataFactory.INVALID_PASSWORD);
		userTo.setCaptchaCode("test");
		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		// given a mocked captcha service - accepting our captcha
		given(this.captchaAdapter.isCaptchaValid(userTo.getCaptchaCode())).willReturn(Boolean.TRUE);
		// given mocked database backend
		given(this.userRepository.saveAndFlush(any(UserEntity.class))).willReturn(userEntity);

		// when a new user sends a sign-in request
		HttpHeaders httpHeader = RestHelper.buildHttpHeader();
		String requestJson = objectMapper.writeValueAsString(userTo);

		try {
			ResponseEntity<UserTo> responseEntity = restClient.post()
					.uri(Endpoint.REGISTER.getPath())
					.headers(headers -> headers.addAll(httpHeader))  // Set headers
					.body(requestJson)  // Set the request body
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(UserTo.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Conflict to be thrown");


		} catch (HttpClientErrorException e) {
			// then we should get a 409 as conflict message (because of the weak password)
			assertThat("Password Policy failed", e.getStatusCode().equals(HttpStatus.CONFLICT));
		}

	}


	@Test // REST-API
	public void testUserGetsWelcomeMailOnValidation() throws Exception {
		// Given a user
		UserTo userTo = new UserTo("test@bluewhale.de", "tester", "test");
		userTo.setValidationToken("validPass");
		UserEntity userEntity = userMapper.mapUserTo2Entity(userTo);

		// Give some Mocks
		given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userEntity);

		// When user clicks in the validation link
		HttpHeaders authedHeader = new HttpHeaders();
		authedHeader.setContentType(MediaType.TEXT_PLAIN);

		String uri = String.format("/api/auth/email/%s/validation/%s", userTo.getEmail(), userTo.getValidationToken());
		ResponseEntity<String> stringResponseEntity = restClient.get().uri(uri)
				.headers(headers -> headers.addAll(authedHeader))
				.retrieve()
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				}).toEntity(String.class);

		// and an should get a confirmative welcome mail must have been sent
		SmtpMessage smtpMessage = ((SmtpMessage) smtpServer.getReceivedEmail().next());
		assertThat(smtpMessage.getBody(), containsString("Your account has been activated."));

	}

	@Test // REST-API
	public void testInvalidatedUserCanNotSignIn() throws Exception {
		// Given
		String plain_password = "test";
		String encrypted_Password = passwordEncoder.encode(plain_password);
		UserTo userTo = new UserTo("test@bluewhale.de", "Tester", encrypted_Password);
		userTo.setValidated(false);
		UserEntity userFromDatabase = userMapper.mapUserTo2Entity(userTo);

		AccountCredentialsTo accountCredentialsTo = new AccountCredentialsTo();
		accountCredentialsTo.setUsername(userFromDatabase.getEmail());
		accountCredentialsTo.setPassword(plain_password);

		// required mocks
		given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userFromDatabase);

		// When user tries to sign-In
		HttpHeaders httpHeaders = RestHelper.buildHttpHeader();
		String requestJson = objectMapper.writeValueAsString(accountCredentialsTo);

		try {

			ResponseEntity<String> responseEntity = restClient.post()
					.uri(Endpoint.LOGIN.getPath())
					.headers(headers -> headers.addAll(httpHeaders))  // Set headers
					.body(requestJson)  // Set the request body
					.retrieve()  // Executes the request and retrieves the response
					.toEntity(String.class);  // Converts the response to a ResponseEntity

			// In case no exception is thrown, fail the test
			fail("Expected HttpClientErrorException$Unauthorized to be thrown");


		} catch (HttpClientErrorException e) {

			// Then we expect a 401
			assertThat("Invalidated User must not be allowed to login!", e.getStatusCode().equals(HttpStatus.UNAUTHORIZED));

		}
	}

	@Test // REST-API
	public void testSuccessfulSignIn() throws Exception {
		// Given
		String plain_password = "test";
		String encrypted_Password = passwordEncoder.encode(plain_password);
		UserTo userTo = new UserTo("test@bluewhale.de", "Tester", encrypted_Password);
		userTo.setValidated(true);
		UserEntity userFromDatabase = userMapper.mapUserTo2Entity(userTo);
		userFromDatabase.setPassword(encrypted_Password); // mapper excludes the password

		AccountCredentialsTo accountCredentialsTo = new AccountCredentialsTo();
		accountCredentialsTo.setUsername(userFromDatabase.getEmail());
		accountCredentialsTo.setPassword(plain_password);

		// required mocks
		given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userFromDatabase);

		// When user tries to sign-In
		HttpHeaders httpHeaders = RestHelper.buildHttpHeader();
		String requestJson = objectMapper.writeValueAsString(accountCredentialsTo);

		ResponseEntity<String> responseEntity = restClient.post()
				.uri(Endpoint.LOGIN.getPath())
				.headers(headers -> headers.addAll(httpHeaders))  // Set headers
				.body(requestJson)  // Set the request body
				.retrieve()  // Executes the request and retrieves the response
				.onStatus(status -> status.value() != 202, (request, response) -> {
					// then we should get a 202 as result.
					throw new RuntimeException("Retrieved wrong status code: " + response.getStatusCode());
				})
				.toEntity(String.class);  // Converts the response to a ResponseEntity
	}

}
