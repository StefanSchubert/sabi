/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

/**
 * ExceptionCodes that may arise during use cases dealing with measurement context.
 *
 * @author schubert
 */
public enum MeasurementExceptionCodes implements ExceptionCode {

    AUTHENTICATION_FAILED(401),
    MEASUREMENT_NOT_FOUND_OR_DOES_NOT_BELONG_TO_USER(409);
    private int errorCode;

    MeasurementExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
