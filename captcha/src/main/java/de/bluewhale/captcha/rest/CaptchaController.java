/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.rest;

import de.bluewhale.captcha.model.ChallengeTo;
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

    @ApiOperation("/challenge")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Robot Challenge Probe.",
                    response = ChallengeTo.class)
    })
    @RequestMapping(value = "/challenge", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ChallengeTo> getNewCaptchaChallenge(@RequestParam(value = "language",
            required = true, defaultValue = "en") String language) {

        ChallengeTo challengeTo = generator.provideChallengeFor(language);
        return new ResponseEntity<ChallengeTo>(challengeTo, HttpStatus.OK);
    }


    /**
     * Checks if a given answer is valid, so that the requested business service may continue.
     *
     * @param captchaChoice Refers to users answer according to {@link ChallengeTo}
     * @return <ul>
     *     <li>{@link HttpStatus#CONTINUE} if code was ok,
     *     <li>{@link HttpStatus#BAD_REQUEST} if no code was provided,
     *     <li>{@link HttpStatus#NOT_ACCEPTABLE} if the code was either already consumed or just wrong.
     *     </ul>
     */
    @ApiOperation("/check")
    @ApiResponses({
            @ApiResponse(code = 400, message = "Missing code parameter."),
            @ApiResponse(code = 202, message = "Answer accepted, continue with registration process."),
            @ApiResponse(code = 406, message = "Wrong Answer. Wrong or expired code. Retry with a new captcha request.")
    })
    @RequestMapping(value = "/check", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<String> checkAnswer(@RequestParam(value = "code", required = true)
                                      @ApiParam(name="code", value="Code of a correct answer to a challenge question." ) String captchaChoice) {
        if (captchaChoice == null || captchaChoice.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST.getReasonPhrase(), HttpStatus.BAD_REQUEST);
        }
        else {
            boolean validCode = checker.probeCode(captchaChoice);
            if (validCode == true) {
                return new ResponseEntity<>(HttpStatus.ACCEPTED.getReasonPhrase(), HttpStatus.ACCEPTED);
            }
            else {
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(), HttpStatus.NOT_ACCEPTABLE);
            }
        }

    }


}
