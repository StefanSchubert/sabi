/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * ExceptionCodes that may arise during use cases dealing with Tank contexts.
 *
 * @author schubert
 */
public enum TankExceptionCodes implements ExceptionCode {

    CREATION_FAILED(1),
    ADDING_FISH_FAILED(2),
    FRAUD_DETECTION(999);

    private int errorCode;

    TankExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
