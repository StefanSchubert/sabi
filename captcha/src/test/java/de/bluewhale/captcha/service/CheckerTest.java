/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.configs.AppConfig;
import org.junit.Assert;
import org.junit.Before;
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

    final String BASE_API_URL = "http://localhost:8081/captcha/api";

    @Autowired
    public Environment env;

    @Value("${challenge.throttle.per.minute}")
    private int MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE;

    private long configuredTTL;

    @PostConstruct // env won't be initialized earlier
    public void lazyInit() {
        String ttl = env.getProperty("token.TTL");
        configuredTTL = Long.parseLong(ttl);
    }

    @Before
    public void resetThresholdMeter(){
        ChallengeRequestThrottle.resetAPICounter();
        ChallengeRequestThrottle.setThrottleThreshold(MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE);
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
        String checkURI = BASE_API_URL + "/check/{code}";
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
        String checkURI = BASE_API_URL + "/challenge/de";
        RestTemplate restTemplate = new RestTemplate();

        // When (Client check)
        while (too_many_requests <= MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE) {
            too_many_requests++;
            try {
                String forObject = restTemplate.getForObject(checkURI, String.class);
            } catch (RestClientException e) {
                // 429 - TOO MANY Request Exception
                Assert.assertTrue(e.getMessage().startsWith("429"));
            }
        }

        // Then (be Happy)
        Assert.assertEquals("Throttle ignition too early.", MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE + 1, too_many_requests);
    }


    @Test
    public void testResetAPIThrottle() throws Exception {
        // This test demonstrates releasing the throttle after a minute

        // Given
        int requestCount = MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE + 1;
        String checkURI = BASE_API_URL + "/challenge/de";
        RestTemplate restTemplate = new RestTemplate();
        ChallengeRequestThrottle.resetAPICounter();

        // When (exceeding the limit)
        for (int i = 0; i <= MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE * 2; i++) {
            try {
                restTemplate.getForObject(checkURI, String.class);
            } catch (RestClientException e) {
                // 429 - TOO MANY Request Exception
                Assert.assertTrue(e.getMessage().startsWith("429"));
                break;
            }
        }

        // Wait a good minute to relax the threshold counter
        Thread.sleep(1000 * 61);

        // Then (should work without throwing excepting)
        try {
            String object = restTemplate.getForObject(checkURI, String.class);
            Assert.assertTrue("Did not received a valid captcha.", object.length() > 10);
        } catch (Exception e) {
            Assert.fail("Last call should have been accepted.");
        }
    }


    @Test
    public void testThrottleMechanism() throws Exception {

        // Given
        String checkURI = BASE_API_URL + "/challenge/de";
        RestTemplate restTemplate = new RestTemplate();

        // When (exceeding the limit)
        for (int i = 0; i <= MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE * 2; i++) {
            try {
                restTemplate.getForObject(checkURI, String.class);
            } catch (RestClientException e) {
                // 429 - TOO MANY Request Exception
                Assert.assertTrue(e.getMessage().startsWith("429"));
                break;
            }
        }

        // Then
        Assert.assertFalse("Throttel not activated.", ChallengeRequestThrottle.requestAllowed());

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