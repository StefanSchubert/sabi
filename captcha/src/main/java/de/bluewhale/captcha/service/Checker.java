/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import org.springframework.stereotype.Service;

/**
 * Responsible to check different captcha challenges
 *
 * @author Stefan Schubert
 */
@Service
public class Checker {

    /**
     * Test if a code is being accepted.
     * @param pCaptchaChoice the answer code
     * @return true if code is valid, false otherwise
     */
    public boolean probeCode(final String pCaptchaChoice) {
        boolean result = false;

        if (pCaptchaChoice != null) {
            if (ValidationCache.knowsCode(pCaptchaChoice)) {
                ValidationCache. invalidateCode(pCaptchaChoice);
                result = true;
            }
        }

        return result;
    }

}
