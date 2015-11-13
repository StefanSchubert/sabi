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
    UNKNOWN_USER(3),
    INSUFFICIENT_PERMISSIONS(4),
    USER_REGISTRATION_FAILED(5)
    ;

    private int errorCode;

    AuthExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
