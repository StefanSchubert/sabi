/*
 * Copyright (c) 2016. by Stefan Schubert
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
    CREATE_SUCCEEDED;

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
