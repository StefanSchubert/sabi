/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services.rest;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.services.CaptchaAdapter;
import de.bluewhale.sabi.util.Obfuscator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import static org.mockito.Matchers.eq;


/**
 * Checks user authorization workflows from client point of view.
 * You may consult the test cases, while developing a specific client.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAuthTest {


//    @LocalServerPort
//    private int port;

    static SimpleSmtpServer smtpServer;
    // json mapper
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    CaptchaAdapter captchaAdapter;
    @MockBean
    UserDao userDao;
    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void init() throws NamingException {
        smtpServer = SimpleSmtpServer.start(2525);
    }


    @AfterClass
    public static void tearDownClass() throws Exception {
        smtpServer.stop();
    }


    /**
     * Invalid captcha code results in 412
     *
     * @throws Exception
     */
    @Test
    public void testNewUserRegistrationWithInvalidCaptcha() throws Exception {

        // given a test user
        UserTo newUser = new UserTo("test@bluewhale.de", "test123");
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
     * Requires a running database
     *
     * @throws Exception
     */
    @Test
    // @Transactional // Usually takes care of rollback but does not work here
    // transaction is not being spanned over the restTemplate call.
    // so we mock the database as well here.
    public void testSuccessfulNewUserRegistration() throws Exception {

        // given a test user
        UserTo newUser = new UserTo("test@bluewhale.de", "test123");
        newUser.setCaptchaCode("test");

        // given a mocked captcha service - accepting our captcha
        given(this.captchaAdapter.isCaptchaValid(newUser.getCaptchaCode())).willReturn(Boolean.TRUE);
        // given mocked database backend
        given(this.userDao.create(any(UserTo.class), eq(Obfuscator.encryptPasswordForHeavensSake("test123")))).willReturn(newUser);

        // when a new user sends a sign-in request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestJson = objectMapper.writeValueAsString(newUser);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        ResponseEntity<UserTo> responseEntity = restTemplate.postForEntity("/api/auth/register", entity, UserTo.class);

        // then we should get a 401 as confirmation.
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.CREATED));

        // and an account validation mail must have been sent
        SmtpMessage smtpMessage = ((SmtpMessage) smtpServer.getReceivedEmail().next());
        assertThat(smtpMessage.getHeaderValue("Subject"), containsString("sabi Account Validation"));
    }

}
