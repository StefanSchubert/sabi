/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for invertebrate catalogue management use cases.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateCatalogueExceptionCodes implements ExceptionCode {

    UNKNOWN_USER(1),
    NOT_YOUR_ENTRY(2),
    CATALOGUE_NOT_FOUND(3),
    NOT_ADMIN(4),
    READ_ONLY_REJECTED(5),
    CATALOGUE_NAME_DUPLICATE(6);

    private final int errorCode;

    InvertebrateCatalogueExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}
