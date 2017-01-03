/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.rest;

import de.bluewhale.captcha.model.CaptchaChallengeTo;
import de.bluewhale.captcha.service.Checker;
import de.bluewhale.captcha.service.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provides the REST API of our Captcha Service
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "/captcha")
public class CaptchaController {

    @Autowired
    Checker checker;

    @Autowired
    Generator generator;


    @RequestMapping(value = "/challenge", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CaptchaChallengeTo> getNewCaptchaChallenge(@RequestParam(value = "language",
            required = true, defaultValue = "en") String language) {

        CaptchaChallengeTo captchaChallengeTo = generator.provideChallengeFor(language);
        return new ResponseEntity<CaptchaChallengeTo>(captchaChallengeTo, HttpStatus.OK);
    }


    /**
     * Checks if a given answer is valid, so that the requested business service may continue.
     *
     * @param captchaChoice Refers to users answer according to {@link CaptchaChallengeTo}
     * @return <ul>
     *     <li>{@link HttpStatus#CONTINUE} if code was ok,
     *     <li>{@link HttpStatus#BAD_REQUEST} if no code was provided,
     *     <li>{@link HttpStatus#NOT_ACCEPTABLE} if the code was either already consumed or just wrong.
     *     </ul>
     */
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    @ResponseStatus
    public HttpStatus checkAnswer(@RequestParam(value = "code") String captchaChoice) {
        if (captchaChoice == null) {
            return HttpStatus.BAD_REQUEST;
        }
        else {
            boolean validCode = checker.probeCode(captchaChoice);
            if (validCode == true) {
                return HttpStatus.CONTINUE;
            }
            else {
                return HttpStatus.NOT_ACCEPTABLE;
            }
        }

    }


}
