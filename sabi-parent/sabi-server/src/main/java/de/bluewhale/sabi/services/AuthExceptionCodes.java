/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * ExceptionCodes that may arise during use cases dealing with auth contexts.
 *
 * @author schubert
 */
public enum AuthExceptionCodes implements ExceptionCode {

    AUTHENTICATION_FAILED(1),
    USER_LOCKED(2),
    INSUFFICIENT_PERMISSIONS(3),
    USER_REGISTRATION_FAILED(4),
    SERVICE_UNAVAILABLE(5),
    PW_RESET_FAILED(6);
    private int errorCode;

    AuthExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
