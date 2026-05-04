/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for use cases dealing with fish stock management.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
public enum FishStockExceptionCodes implements ExceptionCode {

    FISH_NOT_FOUND(1),
    FISH_NOT_YOURS(2),
    FISH_HAS_DEPARTURE_RECORD(3),
    FISH_PHOTO_TOO_LARGE(4),
    FISH_PHOTO_INVALID_FORMAT(5),
    AQUARIUM_NOT_YOURS(6);

    private final int errorCode;

    FishStockExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}

