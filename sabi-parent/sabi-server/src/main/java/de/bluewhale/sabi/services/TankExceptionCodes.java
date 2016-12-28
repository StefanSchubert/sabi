/*
 * Copyright (c) 2016. by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * ExceptionCodes that may arise during use cases dealing with Tank contexts.
 *
 * @author schubert
 */
public enum TankExceptionCodes implements ExceptionCode {

    CREATION_FAILED(1);

    private int errorCode;

    TankExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
