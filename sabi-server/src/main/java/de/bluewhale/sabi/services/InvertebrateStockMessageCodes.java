/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for invertebrate stock management use cases.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateStockMessageCodes implements MessageCode {

    INVERT_CREATED,
    INVERT_UPDATED,
    INVERT_DEPARTURE_RECORDED,
    INVERT_DELETED,
    INVERT_PHOTO_UPLOADED,
    CATALOGUE_LINK_REMOVED,
    // Error codes
    INVERT_NOT_FOUND(InvertebrateStockExceptionCodes.INVERT_NOT_FOUND),
    INVERT_NOT_OWNER(InvertebrateStockExceptionCodes.INVERT_NOT_OWNER),
    INVERT_HAS_DEPARTURE_RECORD(InvertebrateStockExceptionCodes.INVERT_HAS_DEPARTURE_RECORD),
    DEPARTURE_DATE_BEFORE_ENTRY(InvertebrateStockExceptionCodes.DEPARTURE_DATE_BEFORE_ENTRY),
    INVERT_PHOTO_TOO_LARGE(InvertebrateStockExceptionCodes.INVERT_PHOTO_TOO_LARGE),
    INVERT_PHOTO_INVALID_FORMAT(InvertebrateStockExceptionCodes.INVERT_PHOTO_INVALID_FORMAT),
    AQUARIUM_NOT_YOURS(InvertebrateStockExceptionCodes.AQUARIUM_NOT_YOURS),
    MARINE_ONLY(InvertebrateStockExceptionCodes.MARINE_ONLY);

    private InvertebrateStockExceptionCodes exceptionCode;

    InvertebrateStockMessageCodes() {
        this.exceptionCode = null;
    }

    InvertebrateStockMessageCodes(InvertebrateStockExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
