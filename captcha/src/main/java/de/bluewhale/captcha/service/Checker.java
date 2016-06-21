package de.bluewhale.captcha.service;

/**
 * Responsible to check different captcha challenges
 *
 * @author Stefan Schubert
 */
public class Checker {

    /**
     * Test if a code is beeing accepted.
     * @param pCaptchaChoice the answer code
     * @return true if code is valid, false otherwise
     */
    public boolean probeCode(final String pCaptchaChoice) {
        boolean result = false;

        if (pCaptchaChoice != null) {
            if (ValidationCache.knowsCode(pCaptchaChoice)) {
                ValidationCache.invalidateCode(pCaptchaChoice);
                result = true;
            }
        }

        return result;
    }

}
