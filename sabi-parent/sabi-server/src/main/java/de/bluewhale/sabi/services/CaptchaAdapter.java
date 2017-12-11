/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

/**
 * This interface is used to communicate with a captcha service.
 * By wrapping this in an own interface it will be easier to use the captcha service of your choice later on.
 *
 * @author Stefan Schubert
 */
public interface CaptchaAdapter {

    /**
     * Used to check if a provided captcha answer was valid.
     * @param captchaAnswer answer to the presented captcha challenge
     * @return true if captcha was valid, false in case it was invalid and NULL in case of communication problems
     *         to the captcha backend system.
     */
    Boolean isCaptchaValid(String captchaAnswer);

}
