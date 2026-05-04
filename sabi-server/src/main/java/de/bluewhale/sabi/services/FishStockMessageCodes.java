/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for fish stock management.
 * Part of 002-fish-stock-catalogue.
 */
public enum FishStockMessageCodes implements MessageCode {

    FISH_CREATED,
    FISH_UPDATED,
    FISH_DEPARTURE_RECORDED,
    FISH_DELETED,
    FISH_PHOTO_UPLOADED,
    CATALOGUE_LINK_REMOVED,
    // Error codes (wrapping ExceptionCodes)
    FISH_NOT_FOUND(FishStockExceptionCodes.FISH_NOT_FOUND),
    FISH_NOT_YOURS(FishStockExceptionCodes.FISH_NOT_YOURS),
    FISH_HAS_DEPARTURE_RECORD(FishStockExceptionCodes.FISH_HAS_DEPARTURE_RECORD),
    FISH_PHOTO_TOO_LARGE(FishStockExceptionCodes.FISH_PHOTO_TOO_LARGE),
    FISH_PHOTO_INVALID_FORMAT(FishStockExceptionCodes.FISH_PHOTO_INVALID_FORMAT),
    AQUARIUM_NOT_YOURS(FishStockExceptionCodes.AQUARIUM_NOT_YOURS);

    private FishStockExceptionCodes exceptionCode;

    FishStockMessageCodes() {
        this.exceptionCode = null;
    }

    FishStockMessageCodes(FishStockExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
