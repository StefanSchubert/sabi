/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.rest;

import de.bluewhale.captcha.model.CaptchaChallengeTo;
import de.bluewhale.captcha.service.Checker;
import de.bluewhale.captcha.service.Generator;
import io.swagger.annotations.*;
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

    @ApiOperation("/challenge")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "language", value ="2 Digit ISO code of the language you require. If the language" +
                    " is not recognized or unsupported 'en' will serve as fallback.", required = true, dataType = "string")

    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Robot Challenge Probe.",
                    response = CaptchaChallengeTo.class)
    })
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
    @ApiOperation("/check")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value ="Code of a correct answer to a challenge question.", required = true, dataType = "string")

    })
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing code parameter."),
            @ApiResponse(code = 200, message = "Answer accepted, continue with registration process."),
            @ApiResponse(code = 406, message = "Wrong Answer. Wrong or expired code. Retry with a new captcha request.")
    })
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    @ResponseStatus
    public HttpStatus checkAnswer(@RequestParam(value = "code") String captchaChoice) {
        if (captchaChoice == null) {
            return HttpStatus.BAD_REQUEST;
        }
        else {
            boolean validCode = checker.probeCode(captchaChoice);
            if (validCode == true) {
                return HttpStatus.OK;
            }
            else {
                return HttpStatus.NOT_ACCEPTABLE;
            }
        }

    }


}
