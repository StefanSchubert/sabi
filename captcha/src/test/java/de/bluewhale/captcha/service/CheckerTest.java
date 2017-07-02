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
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.PostConstruct;

/**
 * Functional and quality tests of the captcha validation
 *
 * @author Stefan Schubert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.JVM)
public class CheckerTest {

    @Autowired
    public Environment env;

    private long configuredTTL;

    @PostConstruct // env won't be initialized earlier
    public void lazyInit() {
        String ttl = env.getProperty("token.TTL");
        configuredTTL = Long.parseLong(ttl);
    }

    /**
     * As other tests may overwrite the TTL you must ensure, that this is the first test running.
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
        Assert.assertEquals("Different than configured TTL?",configuredTTL,ttl);
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
    public void testCleanUpOfStaleTokens() throws Exception {
        // Given
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

        // Because of the expired TTL  the stale one should be unknown by now
        Assert.assertFalse("Cache cleanup failed", ValidationCache.knowsCode(staleToken));
    }

}