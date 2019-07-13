/*
 * Copyright (c) 2019 by Stefan Schubert
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
