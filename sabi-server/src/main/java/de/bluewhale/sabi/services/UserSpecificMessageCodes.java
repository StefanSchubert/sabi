/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * MessageCodes that may arise during use cases dealing with user related issues (i.g. profile setting).
 *
 * @author schubert
 */
public enum UserSpecificMessageCodes implements MessageCode {

    RECORD_ALREADY_EXISTS(UserSpecificExceptionCodes.CREATION_FAILED),
    CREATE_SUCCEEDED,
    UNKOWN_RECORD(UserSpecificExceptionCodes.RECORD_NOT_FOUND),
    NOT_YOUR_RECORD(UserSpecificExceptionCodes.FRAUD_DETECTION),
    UNKNOWN_USER(UserSpecificExceptionCodes.FRAUD_DETECTION),
    UPDATE_SUCCEEDED,
    REMOVAL_SUCCEEDED;

// ------------------------------ FIELDS ------------------------------

    private UserSpecificExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    UserSpecificMessageCodes() {
        exceptionCode = null;
    }

    UserSpecificMessageCodes(UserSpecificExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
