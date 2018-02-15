/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * MessageCodes that may arise during use cases dealing with tanks.
 *
 * @author schubert
 */
public enum TankMessageCodes implements MessageCode {

    TANK_ALREADY_EXISTS(TankExceptionCodes.CREATION_FAILED),
    CREATE_SUCCEEDED,
    FISH_ALREADY_EXISTS(TankExceptionCodes.ADDING_FISH_FAILED),
    NOT_YOUR_TANK(TankExceptionCodes.FRAUD_DETECTION),
    NOT_YOUR_MEASUREMENT(TankExceptionCodes.FRAUD_DETECTION),
    MEASURMENT_ALREADY_DELETED(TankExceptionCodes.MEASURMENT_NOT_FOUND),
    UNKNOWN_USER(TankExceptionCodes.FRAUD_DETECTION),
    UPDATE_SUCCEEDED,
    REMOVAL_SUCCEEDED;

// ------------------------------ FIELDS ------------------------------

    private TankExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    TankMessageCodes() {
        exceptionCode = null;
    }

    TankMessageCodes(TankExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
