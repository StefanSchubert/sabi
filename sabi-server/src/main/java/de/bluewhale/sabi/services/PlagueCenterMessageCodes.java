/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * MessageCodes that may arise during use cases dealing with tanks.
 *
 * @author schubert
 */
public enum PlagueCenterMessageCodes implements MessageCode {

    RECORD_ALREADY_EXISTS(PlagueCenterExceptionCodes.CREATION_FAILED),
    CREATE_SUCCEEDED,
    NOT_YOUR_TANK(PlagueCenterExceptionCodes.FRAUD_DETECTION),
    NOT_YOUR_RECORD(PlagueCenterExceptionCodes.FRAUD_DETECTION),
    RECORD_ALREADY_DELETED(PlagueCenterExceptionCodes.RECORD_NOT_FOUND),
    UNKNOWN_USER(PlagueCenterExceptionCodes.FRAUD_DETECTION),
    UPDATE_SUCCEEDED,
    REMOVAL_SUCCEEDED;

// ------------------------------ FIELDS ------------------------------

    private PlagueCenterExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    PlagueCenterMessageCodes() {
        exceptionCode = null;
    }

    PlagueCenterMessageCodes(PlagueCenterExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
