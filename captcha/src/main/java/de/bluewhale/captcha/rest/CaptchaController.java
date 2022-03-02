/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.rest;

import de.bluewhale.captcha.model.ChallengeTo;
import de.bluewhale.captcha.service.ChallengeRequestThrottle;
import de.bluewhale.captcha.service.Checker;
import de.bluewhale.captcha.service.QAGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    QAGenerator generator;

    @Operation(method = "/challenge/{language}")
    @ApiResponses({
            @ApiResponse(responseCode =  "200", description = "CAPTCHA Probe activated - you may continue with check call."),
            @ApiResponse(responseCode = "429", description = "Max. requests per minute reached, please retry in 60 secs..")
    })
    @RequestMapping(value = "/challenge/{language}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ChallengeTo> getNewCaptchaChallenge(
            @PathVariable(value = "language", required = true)
            @Parameter(name = "language", description = "ISO-639-1 language code - used for i18n in communication.") String language) {

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
    @Operation(method ="/check/{code}")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Answer accepted, continue with registration process."),
            @ApiResponse(responseCode = "406", description = "Wrong Answer. Wrong or expired code. Retry with a new captcha request.")
    })
    @RequestMapping(value = "/check/{code}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> checkAnswer(@PathVariable(value = "code", required = true)
                                              @Parameter(name = "code", description = "Code of a correct answer to a challenge question.") String captchaChoice) {

        boolean validCode = checker.probeCode(captchaChoice);
        if (validCode == true) {
            return new ResponseEntity<>(HttpStatus.ACCEPTED.getReasonPhrase(), HttpStatus.ACCEPTED);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(), HttpStatus.NOT_ACCEPTABLE);
        }

    }
}
