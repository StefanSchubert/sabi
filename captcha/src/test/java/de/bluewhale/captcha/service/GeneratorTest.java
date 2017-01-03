/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.CaptchaChallengeTo;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Functional and quality tests of the captcha controller
 *
 * @author Stefan Schubert
 */
public class GeneratorTest {


    @Test
    public void provideChallengeForDefaultFallbackLanguage() throws Exception {
        Generator generator = new Generator();
        CaptchaChallengeTo captchaChallengeTo = generator.provideChallengeFor("unknown");

        assertNotNull("Generator did not delivered a captcha", captchaChallengeTo);
        assertNotNull("Generator did not delivered a captcha question", captchaChallengeTo.getQuestion());
        assertNotNull("Generator did not delivered captcha answer options", captchaChallengeTo.getAnswers());
        assertNotNull("Generator did not specified the used language", captchaChallengeTo.getLanguage());
        assertEquals("Fallback language mechanism was not working", "en", captchaChallengeTo.getLanguage());
    }


    @Test
    public void testChallengeForRequestedLanguage() throws Exception {
        // Given
        Generator generator = new Generator();

        // When
        CaptchaChallengeTo captchaChallengeTo = generator.provideChallengeFor(Locale.GERMAN.getLanguage());

        // Then
        assertNotNull("Generator did not delivered a captcha", captchaChallengeTo);
        assertNotNull("Generator did not delivered a captcha question", captchaChallengeTo.getQuestion());
        assertNotNull("Generator did not delivered captcha answer options", captchaChallengeTo.getAnswers());
        assertNotNull("Generator did not specified the used language", captchaChallengeTo.getLanguage());
        assertEquals("Did not get my language", "de", captchaChallengeTo.getLanguage());
    }

}