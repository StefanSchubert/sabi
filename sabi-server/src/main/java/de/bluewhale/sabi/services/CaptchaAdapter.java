/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import java.io.IOException;

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
     * @return true if captcha was valid, false in case it was invalid
     * @throws IOException in case of communication problems to the captcha backend system.
     */
    Boolean isCaptchaValid(String captchaAnswer) throws IOException;

}
