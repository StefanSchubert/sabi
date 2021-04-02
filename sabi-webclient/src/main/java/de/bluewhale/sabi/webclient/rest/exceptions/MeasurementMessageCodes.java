/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.rest.exceptions;


import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MeasurementExceptionCodes;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * MessageCodes that may arise by using the Measurement Restservice
 *
 * @author schubert
 */
public enum MeasurementMessageCodes implements MessageCode {

    NO_SUCH_MEAUREMENT(MeasurementExceptionCodes.MEASUREMENT_NOT_FOUND_OR_DOES_NOT_BELONG_TO_USER);

// ------------------------------ FIELDS ------------------------------

    private MeasurementExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    MeasurementMessageCodes() {
        exceptionCode = null;
    }

    MeasurementMessageCodes(MeasurementExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
