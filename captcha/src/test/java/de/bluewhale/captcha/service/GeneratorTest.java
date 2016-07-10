package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.CaptchaChallengeTo;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO STS: Add Description here...
 *
 * @author Stefan Schubert
 */
public class GeneratorTest {


    @Test
    public void provideChallengeForDefaultLanguage() throws Exception {
        Generator generator = new Generator();
        CaptchaChallengeTo captchaChallengeTo = generator.provideChallengeFor("XX");

        assertNotNull("Generator did not delivered a captcha", captchaChallengeTo);
        assertNotNull("Generator did not delivered a captcha question", captchaChallengeTo.getQuestion());
        assertNotNull("Generator did not delivered captcha answer options", captchaChallengeTo.getAnswers());
        assertNotNull("Generator did not specified the used language", captchaChallengeTo.getLanguage());
        assertEquals("Fallback language mechanism was not working", "en", captchaChallengeTo.getLanguage());
    }


    @Test
    public void testChallengeForRequestedLanguage() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    @Test
    public void testExpireTokens() throws Exception {
        // Given
        ValidationCache.setTTL(500l); // 500ms

        // When requesting tokes 1000ms long

        // Then the cache size must be cleand up, meaning that it must contain less than "generated" tokens.
        fail("Complete implementation needed.");
    }
}