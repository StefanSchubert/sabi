/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

/**
 * ExceptionCodes that do not belong to a specific context.
 *
 * @author schubert
 */
public enum CommonExceptionCodes implements ExceptionCode {

    INTERNAL_ERROR(1),
    ;

    private int errorCode;

    CommonExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
