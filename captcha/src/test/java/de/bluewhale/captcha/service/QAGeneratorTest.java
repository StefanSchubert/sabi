/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.ChallengeTo;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        assertNotNull(challengeTo, "Generator did not delivered a captcha");
        assertNotNull(challengeTo.getQuestion(), "Generator did not delivered a captcha question");
        assertNotNull(challengeTo.getAnswers(), "Generator did not delivered captcha answer options");
        assertNotNull(challengeTo.getLanguage(), "Generator did not specified the used language");
        assertEquals("en", challengeTo.getLanguage(), "Fallback language mechanism was not working");    }


    @Test
    public void testChallengeForRequestedLanguage() throws Exception {
        // Given
        QAGenerator generator = new QAGenerator();

        // When
        ChallengeTo challengeTo = generator.provideChallengeFor(Locale.GERMAN.getLanguage());

        // Then
        assertNotNull(challengeTo, "Generator did not delivered a captcha");
        assertNotNull(challengeTo.getQuestion(), "Generator did not delivered a captcha question");
        assertNotNull(challengeTo.getAnswers(), "Generator did not delivered captcha answer options");
        assertNotNull(challengeTo.getLanguage(), "Generator did not specified the used language");
        assertEquals("de", challengeTo.getLanguage(), "Did not get my language");
    }
}