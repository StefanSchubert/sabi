/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.captcha.rest;

import de.bluewhale.captcha.model.ChallengeTo;
import de.bluewhale.captcha.service.ChallengeRequestThrottle;
import de.bluewhale.captcha.service.Checker;
import de.bluewhale.captcha.service.Generator;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provides the REST API of our Captcha Service
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/")
public class CaptchaController {

    @Autowired
    Checker checker;

    @Autowired
    Generator generator;

    @ApiOperation("/challenge/{language}")
    @ApiResponses({
            @ApiResponse(code = 200, message = "CAPTCHA Probe activated - you may continue with check call.",
                    response = ChallengeTo.class),
            @ApiResponse(code = 429, message = "Max. requests per minute reached, please retry in 60 secs..")
    })
    @RequestMapping(value = "/challenge/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ChallengeTo> getNewCaptchaChallenge(
            @PathVariable(value = "language", required = true)
            @ApiParam(name = "language", value = "ISO-639-1 language code - used for i18n in communication.") String language) {

        ResponseEntity<ChallengeTo> response;

        if (ChallengeRequestThrottle.requestAllowed()) {
            ChallengeTo challengeTo = generator.provideChallengeFor(language);
            response = new ResponseEntity<>(challengeTo, HttpStatus.OK);
        } else {
            response = new ResponseEntity<>(new ChallengeTo(), HttpStatus.TOO_MANY_REQUESTS);
        }

        return response;
    }


    /**
     * Checks if a given answer is valid, so that the requested business service may continue.
     *
     * @param captchaChoice Refers to users answer according to {@link ChallengeTo}
     * @return <ul>
     * <li>{@link HttpStatus#CONTINUE} if code was ok,
     * <li>{@link HttpStatus#BAD_REQUEST} if no code was provided,
     * <li>{@link HttpStatus#NOT_ACCEPTABLE} if the code was either already consumed or just wrong.
     * </ul>
     */
    @ApiOperation("/check/{code}")
    @ApiResponses({
            @ApiResponse(code = 202, message = "Answer accepted, continue with registration process."),
            @ApiResponse(code = 406, message = "Wrong Answer. Wrong or expired code. Retry with a new captcha request.")
    })
    @RequestMapping(value = "/check/{code}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> checkAnswer(@PathVariable(value = "code", required = true)
                                              @ApiParam(name = "code", value = "Code of a correct answer to a challenge question.") String captchaChoice) {

        boolean validCode = checker.probeCode(captchaChoice);
        if (validCode == true) {
            return new ResponseEntity<>(HttpStatus.ACCEPTED.getReasonPhrase(), HttpStatus.ACCEPTED);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(), HttpStatus.NOT_ACCEPTABLE);
        }

    }
}
