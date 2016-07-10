package de.bluewhale.captcha.service;

import de.bluewhale.captcha.model.CaptchaChallengeTo;

import java.util.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;



/**
 * The Generator is responsible to create valid question/answer sets.
 * The first implementation provides easy dummy style captcha sets.
 * If really required this is the part which needs to be more intelligent, meaning that
 * the right answer should not be probable throw brute-force and not
 * recognizable by AI approaches.
 *
 * The current implementation contains commonly shared answers, where only
 * the question is being localized.
 *
 * @author Stefan Schubert
 */
public class Generator {
    private static final int TOKEN_SIZE = 5;
    // ------------------------------ FIELDS ------------------------------

    // question -> AnswerSet ->
    static Set<ChallengeData> dataSet = new HashSet<>();

    private static final Random random = new Random();
    private static final String CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ234567890!@#$";


// -------------------------- STATIC METHODS --------------------------

    static {
        // todo This pattern could be dynamically generated on the fly
        // as this is nonsense as the source is open...but for the start it's fine
        ChallengeData challenge1 = new ChallengeData();
        challenge1.questionMap.put(Locale.GERMAN,"Was passt nicht?");
        challenge1.questionMap.put(Locale.ENGLISH,"Which option does not fit");
        challenge1.answerMap.put(FALSE, "A1-B2-C2D");
        challenge1.answerMap.put(FALSE, "K8-V4-W7H");
        challenge1.answerMap.put(TRUE, "J6-N8-9T9");
        challenge1.answerMap.put(FALSE, "V1-J3-Q2P");

        ChallengeData challenge2= new ChallengeData();
        challenge2.questionMap.put(Locale.GERMAN,"Was passt nicht?");
        challenge2.questionMap.put(Locale.ENGLISH,"Which option does not fit");
        challenge2.answerMap.put(FALSE, "Saturn");
        challenge2.answerMap.put(FALSE, "Jupiter");
        challenge2.answerMap.put(TRUE, "Moon");
        challenge2.answerMap.put(FALSE, "Venus");

        ChallengeData challenge3= new ChallengeData();
        challenge3.questionMap.put(Locale.GERMAN,"Welcher Wert muss gering gehalten werden?");
        challenge3.questionMap.put(Locale.ENGLISH,"Which Value is to be minimized?");
        challenge3.answerMap.put(TRUE, "PO4");
        challenge3.answerMap.put(FALSE, "Ca");
        challenge3.answerMap.put(FALSE, "Mg");
        challenge3.answerMap.put(FALSE, "NO3");


        dataSet.add(challenge1);
        dataSet.add(challenge2);
        dataSet.add(challenge3);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Generates a challenge set suitable for requested language.
     * If the language code is not available "en" will serve as default.
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
        }
        catch (Exception pE) {
            locale = Locale.ENGLISH;
        }
        captchaChallengeTo.setLanguage(locale.getLanguage());
        captchaChallengeTo.setQuestion(questionMap.get(locale.getLanguage()));

        // As for the answer map, we generate answer tokens an for th eright one
        // we register it with the ValidationCache.
        HashMap<String, String> answerMap = new HashMap<>(challengeData.answerMap.size());
        Set<Map.Entry<Boolean, String>> answers = challengeData.answerMap.entrySet();
        for (Map.Entry<Boolean, String> answer : answers) {

            String token = createToken(TOKEN_SIZE);
            if (answer.getKey().equals(TRUE)) {
                ValidationCache.registerToken(token);
            }
            answerMap.put(token, answer.getValue());
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
        protected Map<Locale,String> questionMap;
        protected Map<Boolean, String> answerMap; // the one marked with true is the right answer.

        public ChallengeData() {
            this.questionMap = new HashMap<Locale, String>();
            this.answerMap = new HashMap<Boolean, String>();
        }
    }
}
