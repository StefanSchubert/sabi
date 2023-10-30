/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * This config was necessary as a violated JSR303 via @Valid in a Rest-Endpoint signature,
 * which had a .permitALl() in the WebSecorityConfig, did not result in the expected 400 BAD-REQUEST but
 * an 403 FORBIDDEN instead.
 *
 * This might be a flue in the current spring boot version (v3.1.4), as it worked before.
 * So you may reevaluate the requirement of this handler from time to time by executing {@link AquariumIoTControllerTest#testAddInvalidMeasurement}
 *
 * @author Stefan Schubert
 */
@RestControllerAdvice
public class RestJSR303ExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
