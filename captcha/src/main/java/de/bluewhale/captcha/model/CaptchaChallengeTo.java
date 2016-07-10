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
// ------------------------------ FIELDS ------------------------------

    // Maps Key->Answer
    private Map<String,String> answers;

    // The Captcha question
    private String question;

    // Language of provided challenge;
    private String language;

// --------------------- GETTER / SETTER METHODS ---------------------

    public Map<String, String> getAnswers() {
        return this.answers;
    }

    public void setAnswers(Map<String, String> pAnswers) {
        this.answers = pAnswers;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String pLanguage) {
        this.language = pLanguage;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String pQuestion) {
        this.question = pQuestion;
    }
}
