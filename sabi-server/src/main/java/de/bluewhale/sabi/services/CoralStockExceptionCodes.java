/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for coral stock management use cases.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public enum CoralStockExceptionCodes implements ExceptionCode {

    CORAL_NOT_FOUND(1),
    NOT_YOUR_CORAL(2),
    CORAL_HAS_DEPARTURE_RECORD(3),
    PHOTO_TOO_LARGE(4),
    PHOTO_INVALID_FORMAT(5),
    AQUARIUM_NOT_YOURS(6),
    MARINE_ONLY(7),
    DEPARTURE_DATE_BEFORE_ENTRY(8),
    GROWTH_DATE_AFTER_DEPARTURE(9),
    POLYP_DATE_AFTER_DEPARTURE(10),
    BRANCH_COUNT_MUST_BE_INTEGER(11);

    private final int errorCode;

    CoralStockExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}

