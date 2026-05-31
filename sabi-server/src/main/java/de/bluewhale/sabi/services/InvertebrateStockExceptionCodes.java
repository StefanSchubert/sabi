/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for invertebrate stock management use cases.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateStockExceptionCodes implements ExceptionCode {

    INVERT_NOT_FOUND(1),
    INVERT_NOT_OWNER(2),
    INVERT_HAS_DEPARTURE_RECORD(3),
    DEPARTURE_DATE_BEFORE_ENTRY(4),
    INVERT_PHOTO_TOO_LARGE(5),
    INVERT_PHOTO_INVALID_FORMAT(6),
    AQUARIUM_NOT_YOURS(7),
    MARINE_ONLY(8);

    private final int errorCode;

    InvertebrateStockExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}
