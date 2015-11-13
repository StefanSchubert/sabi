package de.bluewhale.sabi.exception;

import de.bluewhale.sabi.exception.ExceptionCode;

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
