/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.configs.AppConfig;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Functional and quality tests of the captcha validation
 *
 * @author Stefan Schubert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckerTest {

    @Autowired
    public Environment env;

    @Value("${challenge.throttle.per.minute}")
    private long MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE;

    private long configuredTTL;

    @PostConstruct // env won't be initialized earlier
    public void lazyInit() {
        String ttl = env.getProperty("token.TTL");
        configuredTTL = Long.parseLong(ttl);
    }

    /**
     * As other tests may overwrite the TTL you must ensure, that this is the first test running.
     *
     * @throws Exception
     */
    @Test
    public void testTTLInit() throws Exception {
        // Given

        // When
        long ttl = ValidationCache.getTTL();

        // Then
        Assert.assertTrue("application.properties from testclass not accessed?", configuredTTL > 0);
        Assert.assertTrue("application.properties from ValidationCache not accessed?", ttl > 0);
        Assert.assertEquals("Different than configured TTL?", configuredTTL, ttl);
    }


    @Test
    public void validateAnswer() throws Exception {
        // Given
        String validToken = "jhk7786";
        ValidationCache.registerToken(validToken);

        // When
        Assert.assertTrue("Register of token failed", ValidationCache.knowsCode(validToken));
        ValidationCache.invalidateCode(validToken);

        // Then
        Assert.assertFalse("Token was not consumed", ValidationCache.knowsCode(validToken));
    }


    @Test
    public void validateAnswerViaRestCall() throws Exception {
        // This test demonstrates the code a client needs to send a check request

        // Given (Simulate a previous taken challenge request)
        String checkURI = "http://localhost:8081/captcha/api/check/{code}";
        String validToken = "GreenTea";
        ValidationCache.registerToken(validToken);

        // When (Client check)
        RestTemplate restTemplate = new RestTemplate();
        Map params = new HashMap<String, String>(1);
        params.put("code", validToken);
        final String checkresult = restTemplate.getForObject(checkURI, String.class, params);

        // Then (be Happy)
        Assert.assertEquals("Ouch - Token was not recognized", "Accepted", checkresult);
    }

    @Test
    public void testAPIThrottle() throws Exception {
        // Demonstrating effective throttleing
        long too_many_requests = 0;


        // Given
        boolean exceptionOccured = false;
        String checkURI = "http://localhost:8081/captcha/api/challenge/de";
        RestTemplate restTemplate = new RestTemplate();

        // When (Client check)
        while (too_many_requests <= MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE) {
            too_many_requests++;
            try {
                restTemplate.getForObject(checkURI, String.class);
            } catch (RestClientException e) {
                exceptionOccured = true;
            }
        }

        // Then (be Happy)
        Assert.assertTrue("Throtteling does not work", exceptionOccured);
        Assert.assertEquals("Too early throttle ignition", MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE + 1, too_many_requests);
    }


    @Test
    public void testResetAPIThrottle() throws Exception {
        // This test demonstrates releasing the throttle after a minute
        long too_many_requests = 0;


        // Given
        boolean exceptionOccured = false;
        String checkURI = "http://localhost:8081/captcha/api/challenge/de";
        RestTemplate restTemplate = new RestTemplate();
        ChallengeRequestThrottle.resetAPICounter();

        // When (Client check)
        while (too_many_requests <= MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE) {
            too_many_requests++;
            try {
                restTemplate.getForObject(checkURI, String.class);
            } catch (RestClientException e) {
                exceptionOccured = true;
                Thread.sleep(1000 * 61);
            }
        }
        Assert.assertTrue("precondition not satisfied", exceptionOccured);


        // Then (should work without throwing excepting)
        String object = restTemplate.getForObject(checkURI, String.class);
        Assert.assertTrue("Did not retrievd a valid captcha", object.length() > 10);

    }


    @Test
    public void testThrottleMechanism() throws Exception {

        // Given (Simulate a previous taken challenge request)
        boolean result = false;
        ChallengeRequestThrottle.resetAPICounter();

        // When (Client check)
        for (int i = 1; i < MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE; i++) {
            result = ChallengeRequestThrottle.requestAllowed();
        }

        // Then (be Happy)
        Assert.assertTrue("False positive Throtteling?", result);
    }


    @Test
    public void testCleanUpOfStaleTokens() throws Exception {
        // Given
        long old_ttl = ValidationCache.getTTL();
        ValidationCache.setTTL(100l); // 100ms
        String staleToken = "staleToken7z987";
        ValidationCache.registerToken(staleToken);

        // When register new one after expired TTL and Cachesize > Cleanup Threshold Policy
        for (int i = 0; i <= ValidationCache.CLEANUP_THRESHOLD; i++) {
            ValidationCache.registerToken(staleToken + i);
        }
        Thread.sleep(300l);
        String newToken = "nextToken";
        ValidationCache.registerToken(newToken);

        ValidationCache.setTTL(old_ttl); // restablish to avoid conflicts with other tests,
        // Because of the expired TTL  the stale one should be unknown by now
        Assert.assertFalse("Cache cleanup failed", ValidationCache.knowsCode(staleToken));
    }

}