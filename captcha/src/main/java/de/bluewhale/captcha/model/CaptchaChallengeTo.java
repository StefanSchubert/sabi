package de.bluewhale.captcha.model;

import java.util.Map;

/**
 * Represents a captcha challenge.
 * A Challenge contains a single question and a Map of possible answers, each identified by a key.
 * The client is supposed to select the correct answer key, which is randomly generated and valid only once.
 *
 * @author Stefan Schubert
 */
public class CaptchaChallengeTo {

    // Maps Key->Answer
    private Map<String,String> answers;

    // The Captcha Question
    private String question;


}
