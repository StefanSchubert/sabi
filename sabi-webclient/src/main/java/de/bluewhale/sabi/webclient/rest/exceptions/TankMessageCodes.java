/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.rest.exceptions;


import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;
import de.bluewhale.sabi.exception.TankExceptionCodes;

/**
 * MessageCodes that may arise by using the Tank Restservice
 *
 * @author schubert
 */
public enum TankMessageCodes implements MessageCode {

    NO_SUCH_TANK(TankExceptionCodes.TANK_NOT_FOUND_OR_DOES_NOT_BELONG_TO_USER);

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
