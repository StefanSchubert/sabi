/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for coral stock management use cases.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public enum CoralStockMessageCodes implements MessageCode {

    CORAL_CREATED,
    CORAL_UPDATED,
    CORAL_DEPARTURE_RECORDED,
    CORAL_DELETED,
    CORAL_PHOTO_UPLOADED,
    CATALOGUE_LINK_REMOVED,
    // Error codes
    CORAL_NOT_FOUND(CoralStockExceptionCodes.CORAL_NOT_FOUND),
    NOT_YOUR_CORAL(CoralStockExceptionCodes.NOT_YOUR_CORAL),
    CORAL_HAS_DEPARTURE_RECORD(CoralStockExceptionCodes.CORAL_HAS_DEPARTURE_RECORD),
    PHOTO_TOO_LARGE(CoralStockExceptionCodes.PHOTO_TOO_LARGE),
    PHOTO_INVALID_FORMAT(CoralStockExceptionCodes.PHOTO_INVALID_FORMAT),
    AQUARIUM_NOT_YOURS(CoralStockExceptionCodes.AQUARIUM_NOT_YOURS),
    MARINE_ONLY(CoralStockExceptionCodes.MARINE_ONLY),
    DEPARTURE_DATE_BEFORE_ENTRY(CoralStockExceptionCodes.DEPARTURE_DATE_BEFORE_ENTRY),
    GROWTH_DATE_AFTER_DEPARTURE(CoralStockExceptionCodes.GROWTH_DATE_AFTER_DEPARTURE),
    POLYP_DATE_AFTER_DEPARTURE(CoralStockExceptionCodes.POLYP_DATE_AFTER_DEPARTURE),
    BRANCH_COUNT_MUST_BE_INTEGER(CoralStockExceptionCodes.BRANCH_COUNT_MUST_BE_INTEGER);

    private CoralStockExceptionCodes exceptionCode;

    CoralStockMessageCodes() {
        this.exceptionCode = null;
    }

    CoralStockMessageCodes(CoralStockExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
