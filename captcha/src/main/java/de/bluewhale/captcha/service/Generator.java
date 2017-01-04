/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.CaptchaChallengeTo;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


/**
 * The Generator is responsible to create valid question/answer sets.
 * The first implementation provides easy dummy style captcha sets.
 * If really required this is the part which needs to be more intelligent, meaning that
 * the right answer should not be probable throw brute-force and not
 * recognizable by AI approaches.
 * <p>
 * The current implementation contains commonly shared answers, where only
 * the question is being localized.
 *
 * @author Stefan Schubert
 */
@Service
public class Generator {
    private static final int TOKEN_SIZE = 5;
    // ------------------------------ FIELDS ------------------------------
    private static final Random random = new Random();
    private static final String CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890!@#$";
    // question -> AnswerSet ->
    static Set<ChallengeData> dataSet = new HashSet<>();


// -------------------------- STATIC METHODS --------------------------

    static {
        // This pattern challenge1 could be dynamically generated on the fly
        // as this is nonsense as the source is open...but for the start it's fine
        ChallengeData challenge1 = new ChallengeData();
        challenge1.questionMap.put(Locale.GERMAN, "Was passt nicht?");
        challenge1.questionMap.put(Locale.ENGLISH, "Which option does not fit");
        challenge1.answerMap.put("A1-B2-C2D", FALSE);
        challenge1.answerMap.put("K8-V4-W7H", FALSE);
        challenge1.answerMap.put("J6-N8-9T9", TRUE);
        challenge1.answerMap.put("V1-J3-Q2P", FALSE);

        ChallengeData challenge2 = new ChallengeData();
        challenge2.questionMap.put(Locale.GERMAN, "Was passt nicht?");
        challenge2.questionMap.put(Locale.ENGLISH, "Which option does not fit");
        challenge2.answerMap.put("Saturn", FALSE);
        challenge2.answerMap.put("Jupiter", FALSE);
        challenge2.answerMap.put("Moon", TRUE);
        challenge2.answerMap.put("Venus", FALSE);

        ChallengeData challenge3 = new ChallengeData();
        challenge3.questionMap.put(Locale.GERMAN, "Welcher Wert muss gering gehalten werden?");
        challenge3.questionMap.put(Locale.ENGLISH, "Which Value is to be minimized?");
        challenge3.answerMap.put("PO4", TRUE);
        challenge3.answerMap.put("Ca", FALSE);
        challenge3.answerMap.put("Mg", FALSE);
        challenge3.answerMap.put("NO3", FALSE);   
        
        ChallengeData challenge4 = new ChallengeData();
        challenge4.questionMap.put(Locale.GERMAN, "Was ist das Ergebnis?");
        challenge4.questionMap.put(Locale.ENGLISH, "Which is the outcome?");
        challenge4.answerMap.put("+-0", TRUE);
        challenge4.answerMap.put("#-#", FALSE);
        challenge4.answerMap.put("+##", FALSE);
        challenge4.answerMap.put("##0", FALSE);

        ChallengeData challenge5 = new ChallengeData();
        challenge5.questionMap.put(Locale.GERMAN, "Was passt nicht?");
        challenge5.questionMap.put(Locale.ENGLISH, "Which option does not fit");
        challenge5.answerMap.put(":-)", FALSE);
        challenge5.answerMap.put(";-)", FALSE);
        challenge5.answerMap.put(":-(", TRUE);
        challenge5.answerMap.put(":-x", FALSE);

        ChallengeData challenge6 = new ChallengeData();
        challenge6.questionMap.put(Locale.GERMAN, "Platform 9 3/4 was passt dazu am besten?");
        challenge6.questionMap.put(Locale.ENGLISH, "Platform 9 3/4 which association fits best?");
        challenge6.answerMap.put("@@@@@", FALSE);
        challenge6.answerMap.put("#####", FALSE);
        challenge6.answerMap.put("+++++", FALSE);
        challenge6.answerMap.put("=====", TRUE);

        ChallengeData challenge7 = new ChallengeData();
        challenge7.questionMap.put(Locale.GERMAN, "Was würdest du vermehren?");
        challenge7.questionMap.put(Locale.ENGLISH, "Which one would you increase?");
        challenge7.answerMap.put("@@@", FALSE);
        challenge7.answerMap.put("§§§", FALSE);
        challenge7.answerMap.put("%%%", FALSE);
        challenge7.answerMap.put("$$$", TRUE);

        ChallengeData challenge8 = new ChallengeData();
        challenge8.questionMap.put(Locale.GERMAN, "Was nimmt mehr Fläche ein?");
        challenge8.questionMap.put(Locale.ENGLISH, "Which one occupies the bigger space?");
        challenge8.answerMap.put("C", FALSE);
        challenge8.answerMap.put("W", TRUE);
        challenge8.answerMap.put("I", FALSE);
        challenge8.answerMap.put("L", FALSE);

        ChallengeData challenge9 = new ChallengeData();
        challenge9.questionMap.put(Locale.GERMAN, "Welche Option beschreibt den Operator?");
        challenge9.questionMap.put(Locale.ENGLISH, "Which option describes the operator?");
        challenge9.answerMap.put("8? = 64", FALSE);
        challenge9.answerMap.put("? ~ X * X", TRUE);
        challenge9.answerMap.put("4? = 16 ", FALSE);
        challenge9.answerMap.put("? ~ X + X", FALSE);

        ChallengeData challenge10 = new ChallengeData();
        challenge10.questionMap.put(Locale.GERMAN, "Was wird immer besser ereichbar?");
        challenge10.questionMap.put(Locale.ENGLISH, "Which one get's more and more reachable?");
        challenge10.answerMap.put("Jupiter", FALSE);
        challenge10.answerMap.put("Mars", TRUE);
        challenge10.answerMap.put("Venus", FALSE);
        challenge10.answerMap.put("Saturn", FALSE);


        dataSet.add(challenge1);
        dataSet.add(challenge2);
        dataSet.add(challenge3);
        dataSet.add(challenge4);
        dataSet.add(challenge5);
        dataSet.add(challenge6);
        dataSet.add(challenge7);
        dataSet.add(challenge8);
        dataSet.add(challenge9);
        dataSet.add(challenge10);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Generates a challenge set suitable for requested language.
     * If the language code is not available "en" will serve as default.
     *
     * @param pLanguage 2char language code
     * @return Challenge object, consisting of the question and a set of answers and their one-pass submission key.
     */
    public CaptchaChallengeTo provideChallengeFor(final String pLanguage) {

        int questionIndex = random.nextInt(dataSet.size());

        Object[] challenges = dataSet.toArray();
        ChallengeData challengeData = (ChallengeData) challenges[questionIndex];

        CaptchaChallengeTo captchaChallengeTo = new CaptchaChallengeTo();

        Map<Locale, String> questionMap = challengeData.questionMap;
        Locale locale;

        try {
            locale = new Locale(pLanguage);
            locale.getISO3Language(); // semantic check
        } catch (MissingResourceException pE) {
            locale = Locale.ENGLISH;
        }
        captchaChallengeTo.setLanguage(locale.getLanguage());
        captchaChallengeTo.setQuestion(questionMap.get(locale));

        // As for the answer map, we generate answer tokens and for the right one
        // we register it with the ValidationCache.
        HashMap<String, String> answerMap = new HashMap<>(challengeData.answerMap.size());
        Set<Map.Entry<String, Boolean>> answers = challengeData.answerMap.entrySet();
        for (Map.Entry<String, Boolean> answer : answers) {

            String token = createToken(TOKEN_SIZE);
            if (answer.getValue().equals(TRUE)) {
                ValidationCache.registerToken(token);
            }
            answerMap.put(token, answer.getKey());
        }
        captchaChallengeTo.setAnswers(answerMap);

        return captchaChallengeTo;
    }

    // Provides a unique Token
    private String createToken(int pTOKEN_SIZE) {
        StringBuilder token = new StringBuilder(pTOKEN_SIZE);
        for (int i = 0; i < pTOKEN_SIZE; i++) {
            token.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return token.toString();
    }

// -------------------------- INNER CLASSES --------------------------

    static private class ChallengeData {
        protected Map<Locale, String> questionMap;
        protected Map<String, Boolean> answerMap; // the one marked with true is the right answer.

        public ChallengeData() {
            this.questionMap = new HashMap<Locale, String>();
            this.answerMap = new HashMap<String, Boolean>();
        }
    }
}
