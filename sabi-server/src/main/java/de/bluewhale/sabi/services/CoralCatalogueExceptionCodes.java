/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for coral catalogue management use cases.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public enum CoralCatalogueExceptionCodes implements ExceptionCode {

    UNKNOWN_USER(1),
    NOT_YOUR_ENTRY(2),
    ENTRY_NOT_FOUND(3),
    NOT_ADMIN(4),
    READ_ONLY_REJECTED(5);

    private final int errorCode;

    CoralCatalogueExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}

