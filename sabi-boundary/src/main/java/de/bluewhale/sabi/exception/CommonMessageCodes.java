/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

/**
 * MessageCodes that may arise during use cases dealing with authentication or authorization.
 *
 * @author schubert
 */
public enum CommonMessageCodes implements MessageCode {
    INSUFFICIENT_DATA(CommonExceptionCodes.DATA_INCOMPLETE),
    UPDATE_SUCCEEDED;

// ------------------------------ FIELDS ------------------------------

    private CommonExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    CommonMessageCodes() {
        exceptionCode = null;
    }

    CommonMessageCodes(CommonExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
