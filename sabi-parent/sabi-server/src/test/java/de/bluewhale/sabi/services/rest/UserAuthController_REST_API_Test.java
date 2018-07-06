/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services.rest;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.AccountCredentialsTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.services.CaptchaAdapter;
import de.bluewhale.sabi.util.Mapper;
import de.bluewhale.sabi.util.Obfuscator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import javax.naming.NamingException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;


/**
 * Checks user authorization workflows from client point of view.
 * You may consult the test cases, while developing a specific client.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAuthController_REST_API_Test {


    SimpleSmtpServer smtpServer;

    @Autowired
    ObjectMapper objectMapper;  // json mapper

    @MockBean
    CaptchaAdapter captchaAdapter;

    @MockBean
    UserRepository userRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void initFakeMailer() throws NamingException {
        smtpServer = SimpleSmtpServer.start(2525);
    }


    @After
    public void stopFakeMailer() throws Exception {
        smtpServer.stop();
    }

    @After
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
        UserTo newUser = new UserTo("test@bluewhale.de", "Tester","test123");
        newUser.setCaptchaCode("captcha mock not programmed so check will be false.");

        // when a new user sends a sign-in request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = objectMapper.writeValueAsString(newUser);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<UserTo> responseEntity = restTemplate.postForEntity("/api/auth/register", entity, UserTo.class);

        // then we should get a 412 as result.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.PRECONDITION_FAILED));
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
        UserTo userTo = new UserTo("test@bluewhale.de", "Tester","test123");
        userTo.setCaptchaCode("test");
        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo,userEntity);

        // given a mocked captcha service - accepting our captcha
        given(this.captchaAdapter.isCaptchaValid(userTo.getCaptchaCode())).willReturn(Boolean.TRUE);
        // given mocked database backend
        given(this.userRepository.saveAndFlush(any(UserEntity.class))).willReturn(userEntity);

        // when a new user sends a sign-in request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestJson = objectMapper.writeValueAsString(userTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<UserTo> responseEntity = restTemplate.postForEntity("/api/auth/register", entity, UserTo.class);

        // then we should get a 401 as confirmation.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // and an account validation mail must have been sent
        SmtpMessage smtpMessage = ((SmtpMessage) smtpServer.getReceivedEmail().next());
        assertThat(smtpMessage.getHeaderValue("Subject"), containsString("sabi Account Validation"));
    }

    @Test // REST-API
    public void testUserGetsWelcomeMailOnValidation() throws Exception {
        // Given a user
        UserTo userTo = new UserTo("test@bluewhale.de", "tester", "test");
        userTo.setValidationToken("validPass");
        UserEntity userEntity = new UserEntity();
        Mapper.mapUserTo2Entity(userTo,userEntity);

        // Give some Mocks
        given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userEntity);

        // When user clicks in the validation link
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/api/auth/email/{email}/validation/{token}",
                HttpMethod.GET,
                httpEntity,
                String.class,
                userTo.getEmail(),
                userTo.getValidationToken()
        );

        // Then we expect a 202 as confirmation
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));

        // and an confirmative welcome mail must have been sent
        SmtpMessage smtpMessage = ((SmtpMessage) smtpServer.getReceivedEmail().next());
        assertThat(smtpMessage.getBody(), containsString("Your account has been activated."));

    }

    // FIXME STS (30.12.17): Needs to be investigated. currently results in an HttpRetryException, instead of retrning a
    // proper status-code. Needs to be adopted in such way, that the user get's a message about the incomplete
    // registration process instead.
    @Test // REST-API
    public void testInvalidatedUserCanNotSignIn() throws Exception {
        // Given
        String plain_password = "test";
        String encrypted_Password = Obfuscator.encryptPasswordForHeavensSake(plain_password);
        UserTo userTo = new UserTo("test@bluewhale.de", "Tester", encrypted_Password);
        userTo.setValidated(false);
        UserEntity userFromDatabase = new UserEntity();
        Mapper.mapUserTo2Entity(userTo,userFromDatabase);


        AccountCredentialsTo accountCredentialsTo = new AccountCredentialsTo();
        accountCredentialsTo.setUsername(userFromDatabase.getEmail());
        accountCredentialsTo.setPassword(plain_password);

        // required mocks
        given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userFromDatabase);

        // When user tries to sign-In
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = objectMapper.writeValueAsString(accountCredentialsTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("/api/auth/login", entity, String.class);

        // Then we expect a 401
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test // REST-API
    public void testSuccessfulSignIn() throws Exception {
        // Given
        String plain_password = "test";
        String encrypted_Password = Obfuscator.encryptPasswordForHeavensSake(plain_password);
        UserTo userTo = new UserTo("test@bluewhale.de", "Tester", encrypted_Password);
        userTo.setValidated(true);
        UserEntity userFromDatabase = new UserEntity();
        Mapper.mapUserTo2Entity(userTo,userFromDatabase);
        userFromDatabase.setPassword(encrypted_Password); // mapper excludes the passwort

        AccountCredentialsTo accountCredentialsTo = new AccountCredentialsTo();
        accountCredentialsTo.setUsername(userFromDatabase.getEmail());
        accountCredentialsTo.setPassword(plain_password);

        // required mocks
        given(this.userRepository.getByEmail(userTo.getEmail())).willReturn(userFromDatabase);

        // When user tries to sign-In
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestJson = objectMapper.writeValueAsString(accountCredentialsTo);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("/api/auth/login", entity, String.class);

        // Then we expect a 401
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.ACCEPTED));
    }


}
