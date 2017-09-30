/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

/**
 * Represents a captcha challenge.
 * A Challenge contains a single question and a Map of possible answers, each identified by a key.
 * The client is supposed to select the correct answer key, which is randomly generated and valid only once.
 *
 * @author Stefan Schubert
 */
public class ChallengeTo {
// ------------------------------ FIELDS ------------------------------

    // Maps Key->Answer
    private Map<String,String> answers;

    // The Captcha question
    private String question;

    // Language of provided challenge;
    private String language;

// --------------------- GETTER / SETTER METHODS ---------------------

    @ApiModelProperty(notes = "List of keys and belonging answers. Only one key is valid and will be accepted.")
    public Map<String, String> getAnswers() {
        return this.answers;
    }

    public void setAnswers(Map<String, String> pAnswers) {
        this.answers = pAnswers;
    }

    @ApiModelProperty(notes = "Served language. If the requested on is not avialable, you will retrieve english as default.")
    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String pLanguage) {
        this.language = pLanguage;
    }

    @ApiModelProperty(notes = "Question which is supposed to sort out robotes.", required = true)
    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String pQuestion) {
        this.question = pQuestion;
    }
}
