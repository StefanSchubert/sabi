/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.ChallengeTo;
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
public class QAGenerator {
    private static final int TOKEN_SIZE = 5;
    // ------------------------------ FIELDS ------------------------------
    private static final Random random = new Random();
    private static final String CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890!@#$";
    // question -> AnswerSet ->
    static Set<ChallengeData> dataSet = new HashSet<>();


// -------------------------- STATIC METHODS --------------------------

    static {
        // The questions from the ressource bundle will be combined with the possible answers in this step.
        // The criteria for new questions is, that the possible answers are as language agnostic as possible.

        ChallengeData challenge1 = new ChallengeData(1);
        challenge1.answerMap.put("A1-B2-C2D", FALSE);
        challenge1.answerMap.put("K8-V4-W7H", FALSE);
        challenge1.answerMap.put("J6-N8-9T9", TRUE);
        challenge1.answerMap.put("V1-J3-Q2P", FALSE);

        ChallengeData challenge2 = new ChallengeData(2);
        challenge2.answerMap.put("Saturn", FALSE);
        challenge2.answerMap.put("Jupiter", FALSE);
        challenge2.answerMap.put("Moon", TRUE);
        challenge2.answerMap.put("Venus", FALSE);

        ChallengeData challenge3 = new ChallengeData(3);
        challenge3.answerMap.put("PO4", TRUE);
        challenge3.answerMap.put("Ca", FALSE);
        challenge3.answerMap.put("Mg", FALSE);
        challenge3.answerMap.put("NO3", FALSE);   
        
        ChallengeData challenge4 = new ChallengeData(4);
        challenge4.answerMap.put("+-0", TRUE);
        challenge4.answerMap.put("#-#", FALSE);
        challenge4.answerMap.put("+##", FALSE);
        challenge4.answerMap.put("##0", FALSE);

        ChallengeData challenge5 = new ChallengeData(5);
        challenge5.answerMap.put(":-)", FALSE);
        challenge5.answerMap.put(";-)", FALSE);
        challenge5.answerMap.put(":-(", TRUE);
        challenge5.answerMap.put(":-x", FALSE);

        ChallengeData challenge6 = new ChallengeData(6);
        challenge6.answerMap.put("@@@@@", FALSE);
        challenge6.answerMap.put("#####", FALSE);
        challenge6.answerMap.put("+++++", FALSE);
        challenge6.answerMap.put("=====", TRUE);

        ChallengeData challenge7 = new ChallengeData(7);
        challenge7.answerMap.put("@@@", FALSE);
        challenge7.answerMap.put("§§§", FALSE);
        challenge7.answerMap.put("%%%", FALSE);
        challenge7.answerMap.put("$$$", TRUE);

        ChallengeData challenge8 = new ChallengeData(8);
        challenge8.answerMap.put("C", FALSE);
        challenge8.answerMap.put("W", TRUE);
        challenge8.answerMap.put("I", FALSE);
        challenge8.answerMap.put("L", FALSE);

        ChallengeData challenge9 = new ChallengeData(9);
        challenge9.answerMap.put("=====", FALSE);
        challenge9.answerMap.put("#####", TRUE);
        challenge9.answerMap.put("NNNNN", FALSE);
        challenge9.answerMap.put("XXXXX", FALSE);

        ChallengeData challenge10 = new ChallengeData(10);
        challenge10.answerMap.put("Jupiter", FALSE);
        challenge10.answerMap.put("Mars", TRUE);
        challenge10.answerMap.put("Venus", FALSE);
        challenge10.answerMap.put("Saturn", FALSE);

        ChallengeData challenge11 = new ChallengeData(11);
        challenge11.answerMap.put("10x10x10 Box", FALSE);
        challenge11.answerMap.put("5x5x5 Box", TRUE);
        challenge11.answerMap.put("4x8x12 Box", FALSE);
        challenge11.answerMap.put("20x20x20 Box", FALSE);

        ChallengeData challenge12 = new ChallengeData(12);
        challenge12.answerMap.put("5cm Ball", TRUE);
        challenge12.answerMap.put("10cm Ball", FALSE);
        challenge12.answerMap.put("15cm Ball", FALSE);
        challenge12.answerMap.put("20cm Ball", FALSE);

        ChallengeData challenge13 = new ChallengeData(13);
        challenge13.answerMap.put("500km NN", TRUE);
        challenge13.answerMap.put("10km NN", FALSE);
        challenge13.answerMap.put("-50m NN (sea)", FALSE);
        challenge13.answerMap.put("5m NN", FALSE);

        ChallengeData challenge14 = new ChallengeData(14);
        challenge14.answerMap.put("Blue Curaçao", FALSE);
        challenge14.answerMap.put("Vodka", TRUE);
        challenge14.answerMap.put("Campari", FALSE);
        challenge14.answerMap.put("Ginger Ale", FALSE);

        ChallengeData challenge15 = new ChallengeData(15);
        challenge15.answerMap.put("Wall-E", FALSE);
        challenge15.answerMap.put("E.T.", TRUE);
        challenge15.answerMap.put("R2D2", FALSE);
        challenge15.answerMap.put("No.5", FALSE);

        ChallengeData challenge16 = new ChallengeData(16);
        challenge16.answerMap.put("\\|/\\|/", FALSE);
        challenge16.answerMap.put("Superfood", FALSE);
        challenge16.answerMap.put("C02-Storage", TRUE);
        challenge16.answerMap.put("=~=~=~~", FALSE);

        ChallengeData challenge17 = new ChallengeData(17);
        challenge17.answerMap.put("9", FALSE);
        challenge17.answerMap.put("4", FALSE);
        challenge17.answerMap.put("22", FALSE);
        challenge17.answerMap.put("5", TRUE);

        ChallengeData challenge18 = new ChallengeData(18);
        challenge18.answerMap.put("☠️", TRUE);
        challenge18.answerMap.put("🦐", FALSE);
        challenge18.answerMap.put("🐡", FALSE);
        challenge18.answerMap.put("🧜🏽‍", FALSE);

        ChallengeData challenge19 = new ChallengeData(19);
        challenge19.answerMap.put("🦐", FALSE);
        challenge19.answerMap.put("🐡", FALSE);
        challenge19.answerMap.put("🧜🏽‍", FALSE);        
        challenge19.answerMap.put("☠️", TRUE);

        ChallengeData challenge20 = new ChallengeData(20);
        challenge20.answerMap.put("🐬", FALSE);
        challenge20.answerMap.put("🐒", FALSE);
        challenge20.answerMap.put("🐢", TRUE);
        challenge20.answerMap.put("🦉", FALSE);

        ChallengeData challenge21 = new ChallengeData(21);
        challenge21.answerMap.put("🪢👀", FALSE);
        challenge21.answerMap.put("🚶‍➡️", TRUE);
        challenge21.answerMap.put("🏄‍💨", FALSE);
        challenge21.answerMap.put("🐋🌎", FALSE);

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
        dataSet.add(challenge11);
        dataSet.add(challenge12);
        dataSet.add(challenge13);
        dataSet.add(challenge14);
        dataSet.add(challenge15);
        dataSet.add(challenge16);
        dataSet.add(challenge17);
        dataSet.add(challenge18);
        dataSet.add(challenge19);
        dataSet.add(challenge20);
        dataSet.add(challenge21);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Generates a challenge set suitable for requested language.
     * If the language code is not available "en" will serve as default.
     *
     * @param pLanguage 2char language code
     * @return Challenge object, consisting of the question and a set of answers and their one-pass submission key.
     */
    public ChallengeTo provideChallengeFor(final String pLanguage) {

        Locale locale;

        try {

            // check for supported locales and set "en" as fallback
            switch (pLanguage) {
                case "de":
                case "en":
                case "fr":
                case "es":
                case "it":
                    locale = new Locale(pLanguage);
                    break;
                default: locale = Locale.ENGLISH;
            }

            locale.getISO3Language(); // semantic check

        } catch (MissingResourceException pE) {
            locale = Locale.ENGLISH;
        }

        ResourceBundle bundle = ResourceBundle.getBundle("i18n/ChallengeSamples", locale);

        int questionIndex = random.nextInt(dataSet.size());

        Object[] challenges = dataSet.toArray();
        ChallengeData challengeData = (ChallengeData) challenges[questionIndex];

        ChallengeTo challengeTo = new ChallengeTo();

        challengeTo.setLanguage(locale.getLanguage());

        String localedQuestion = bundle.getString(challengeData.questionKey);

        challengeTo.setQuestion(localedQuestion);

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
        challengeTo.setAnswers(answerMap);

        return challengeTo;
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

    private static class ChallengeData {
        protected String questionKey;
        protected Map<String, Boolean> answerMap; // the one marked with true is the right answer.

        public ChallengeData(int challengeNumber) {
            this.questionKey = "challenge."+challengeNumber+".question";
            this.answerMap = new HashMap<String, Boolean>();
        }
    }
}
