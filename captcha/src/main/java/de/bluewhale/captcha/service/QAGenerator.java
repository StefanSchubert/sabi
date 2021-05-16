/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
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

    // FIXME: 30.09.18 These data shuld be json config, which is loaded as runtime.
    
    static {
        // This pattern challenge1 could be dynamically generated on the fly
        // as this is nonsense as the source is open...but for the start it's fine
        ChallengeData challenge1 = new ChallengeData();
        challenge1.questionMap.put(Locale.GERMAN, "Welches passt nicht zum Muster?");
        challenge1.questionMap.put(Locale.ENGLISH, "Which one does not fit to the pattern?");
        challenge1.answerMap.put("A1-B2-C2D", FALSE);
        challenge1.answerMap.put("K8-V4-W7H", FALSE);
        challenge1.answerMap.put("J6-N8-9T9", TRUE);
        challenge1.answerMap.put("V1-J3-Q2P", FALSE);

        ChallengeData challenge2 = new ChallengeData();
        challenge2.questionMap.put(Locale.GERMAN, "Was passt nicht?");
        challenge2.questionMap.put(Locale.ENGLISH, "Which option does not fit?");
        challenge2.answerMap.put("Saturn", FALSE);
        challenge2.answerMap.put("Jupiter", FALSE);
        challenge2.answerMap.put("Moon", TRUE);
        challenge2.answerMap.put("Venus", FALSE);

        ChallengeData challenge3 = new ChallengeData();
        challenge3.questionMap.put(Locale.GERMAN, "Welcher Wert muss gering gehalten werden?");
        challenge3.questionMap.put(Locale.ENGLISH, "Which value is to be minimized?");
        challenge3.answerMap.put("PO4", TRUE);
        challenge3.answerMap.put("Ca", FALSE);
        challenge3.answerMap.put("Mg", FALSE);
        challenge3.answerMap.put("NO3", FALSE);   
        
        ChallengeData challenge4 = new ChallengeData();
        challenge4.questionMap.put(Locale.GERMAN, "Was gibt es davon sprachlich?");
        challenge4.questionMap.put(Locale.ENGLISH, "Which one exists likely as spoken words?");
        challenge4.answerMap.put("+-0", TRUE);
        challenge4.answerMap.put("#-#", FALSE);
        challenge4.answerMap.put("+##", FALSE);
        challenge4.answerMap.put("##0", FALSE);

        ChallengeData challenge5 = new ChallengeData();
        challenge5.questionMap.put(Locale.GERMAN, "Wen würdest du aufmuntern?");
        challenge5.questionMap.put(Locale.ENGLISH, "Who needs a little support?");
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
        challenge9.questionMap.put(Locale.GERMAN, "Wenn jeder Strich ein Bleistift wäre und du diese nun in eine Linie setzt, was gibt die längere Strecke?");
        challenge9.questionMap.put(Locale.ENGLISH, "If each line is a pencil and you set them in a line, which one would result in the max line?");
        challenge9.answerMap.put("=====", FALSE);
        challenge9.answerMap.put("#####", TRUE);
        challenge9.answerMap.put("NNNNN", FALSE);
        challenge9.answerMap.put("XXXXX", FALSE);

        ChallengeData challenge10 = new ChallengeData();
        challenge10.questionMap.put(Locale.GERMAN, "Was wird immer besser ereichbar?");
        challenge10.questionMap.put(Locale.ENGLISH, "Which one get's more and more reachable?");
        challenge10.answerMap.put("Jupiter", FALSE);
        challenge10.answerMap.put("Mars", TRUE);
        challenge10.answerMap.put("Venus", FALSE);
        challenge10.answerMap.put("Saturn", FALSE);

        ChallengeData challenge11 = new ChallengeData();
        challenge11.questionMap.put(Locale.GERMAN, "Welche Box passt 27 mal in eine 15x15x15 Box?");
        challenge11.questionMap.put(Locale.ENGLISH, "Which box fits 27 times in a 15x15x15 box?");
        challenge11.answerMap.put("10x10x10 Box", FALSE);
        challenge11.answerMap.put("5x5x5 Box", TRUE);
        challenge11.answerMap.put("4x8x12 Box", FALSE);
        challenge11.answerMap.put("20x20x20 Box", FALSE);

        ChallengeData challenge12 = new ChallengeData();
        challenge12.questionMap.put(Locale.GERMAN, "Welcher Ball rollt am ehsten durch ein Mäuseloch?");
        challenge12.questionMap.put(Locale.ENGLISH, "Which ball is likely to be rolling through a mouse hole?");
        challenge12.answerMap.put("5cm Ball", TRUE);
        challenge12.answerMap.put("10cm Ball", FALSE);
        challenge12.answerMap.put("15cm Ball", FALSE);
        challenge12.answerMap.put("20cm Ball", FALSE);

        ChallengeData challenge13 = new ChallengeData();
        challenge13.questionMap.put(Locale.GERMAN, "Hinter dir kollidieren zwei schnelle Objekte, wo wirst du es am geringsten bemerken?");
        challenge13.questionMap.put(Locale.ENGLISH, "Two fast objects are colliding behind you, where will you hardly recognize it?");
        challenge13.answerMap.put("500km NN", TRUE);
        challenge13.answerMap.put("10km NN", FALSE);
        challenge13.answerMap.put("-50m NN (sea)", FALSE);
        challenge13.answerMap.put("5m NN", FALSE);

        ChallengeData challenge14 = new ChallengeData();
        challenge14.questionMap.put(Locale.GERMAN, "Welches davon kippen manche in ihr Riff-Aquarium?");
        challenge14.questionMap.put(Locale.ENGLISH, "Which one of these will be added by some of us to the reef-tank?");
        challenge14.answerMap.put("Blue Curaçao", FALSE);
        challenge14.answerMap.put("Wodka", TRUE);
        challenge14.answerMap.put("Campari", FALSE);
        challenge14.answerMap.put("Ginger Ale", FALSE);

        ChallengeData challenge15 = new ChallengeData();
        challenge15.questionMap.put(Locale.GERMAN, "Wer fällt aus der Reihe?");
        challenge15.questionMap.put(Locale.ENGLISH, "Who does not fit here?");
        challenge15.answerMap.put("Wall-E", FALSE);
        challenge15.answerMap.put("E.T.", TRUE);
        challenge15.answerMap.put("R2D2", FALSE);
        challenge15.answerMap.put("No.5", FALSE);

        ChallengeData challenge16 = new ChallengeData();
        challenge16.questionMap.put(Locale.GERMAN, "Welche Eigenschaften von Seegraswiesen können uns zukünftig helfen?");
        challenge16.questionMap.put(Locale.ENGLISH, "What are the best capabilities of Seaweed-Cultures for our furture?");
        challenge16.answerMap.put("\\|/\\|/", FALSE);
        challenge16.answerMap.put("Superfood", FALSE);
        challenge16.answerMap.put("C02-Storage", TRUE);
        challenge16.answerMap.put("=~=~=~~", FALSE);

        ChallengeData challenge17 = new ChallengeData();
        challenge17.questionMap.put(Locale.GERMAN, "Welche Zahl ist nur durch eins und sich selbst teilbar, so dass das Ergebnis wieder eine Ganze Zahl ergibt?");
        challenge17.questionMap.put(Locale.ENGLISH, "If the result must be a whole number, which one of those can be divided only by 1 and itself?");
        challenge17.answerMap.put("9", FALSE);
        challenge17.answerMap.put("4", FALSE);
        challenge17.answerMap.put("22", FALSE);
        challenge17.answerMap.put("5", TRUE);

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

        int questionIndex = random.nextInt(dataSet.size());

        Object[] challenges = dataSet.toArray();
        ChallengeData challengeData = (ChallengeData) challenges[questionIndex];

        ChallengeTo challengeTo = new ChallengeTo();

        Map<Locale, String> questionMap = challengeData.questionMap;
        Locale locale;

        try {
            locale = new Locale(pLanguage);
            locale.getISO3Language(); // semantic check
        } catch (MissingResourceException pE) {
            locale = Locale.ENGLISH;
        }
        challengeTo.setLanguage(locale.getLanguage());
        challengeTo.setQuestion(questionMap.get(locale));

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

    static private class ChallengeData {
        protected Map<Locale, String> questionMap;
        protected Map<String, Boolean> answerMap; // the one marked with true is the right answer.

        public ChallengeData() {
            this.questionMap = new HashMap<Locale, String>();
            this.answerMap = new HashMap<String, Boolean>();
        }
    }
}
