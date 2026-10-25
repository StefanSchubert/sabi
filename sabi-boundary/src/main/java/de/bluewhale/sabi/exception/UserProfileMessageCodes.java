/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

/**
 * MessageCodes that may arise during user profile processing.
 *
 * @author schubert
 */
public enum UserProfileMessageCodes implements MessageCode {
    EMAIL_REMINDER_INTERVAL_OUT_OF_RANGE(CommonExceptionCodes.DATA_INCOMPLETE);

// ------------------------------ FIELDS ------------------------------

    private CommonExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    UserProfileMessageCodes(CommonExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
