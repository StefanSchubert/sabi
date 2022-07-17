/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * ExceptionCodes that may arise during use cases dealing with Plague Records.
 *
 * @author schubert
 */
public enum PlagueCenterExceptionCodes implements ExceptionCode {

    CREATION_FAILED(1),
    RECORD_NOT_FOUND(2),
    FRAUD_DETECTION(999);

    private int errorCode;

    PlagueCenterExceptionCodes(int pErrorCode) {
        this.errorCode = pErrorCode;
    }


    public int getErrorCode() {
        return this.errorCode;
    }
}
