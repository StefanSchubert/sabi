/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.ChallengeTo;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Functional and quality tests of the captcha controller
 *
 * @author Stefan Schubert
 */
public class QAGeneratorTest {


    @Test
    public void provideChallengeForDefaultFallbackLanguage() throws Exception {
        QAGenerator generator = new QAGenerator();
        ChallengeTo challengeTo = generator.provideChallengeFor("unknown");

        assertNotNull("Generator did not delivered a captcha", challengeTo);
        assertNotNull("Generator did not delivered a captcha question", challengeTo.getQuestion());
        assertNotNull("Generator did not delivered captcha answer options", challengeTo.getAnswers());
        assertNotNull("Generator did not specified the used language", challengeTo.getLanguage());
        assertEquals("Fallback language mechanism was not working", "en", challengeTo.getLanguage());
    }


    @Test
    public void testChallengeForRequestedLanguage() throws Exception {
        // Given
        QAGenerator generator = new QAGenerator();

        // When
        ChallengeTo challengeTo = generator.provideChallengeFor(Locale.GERMAN.getLanguage());

        // Then
        assertNotNull("Generator did not delivered a captcha", challengeTo);
        assertNotNull("Generator did not delivered a captcha question", challengeTo.getQuestion());
        assertNotNull("Generator did not delivered captcha answer options", challengeTo.getAnswers());
        assertNotNull("Generator did not specified the used language", challengeTo.getLanguage());
        assertEquals("Did not get my language", "de", challengeTo.getLanguage());
    }

}