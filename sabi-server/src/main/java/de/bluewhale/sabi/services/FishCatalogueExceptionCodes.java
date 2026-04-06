/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;

/**
 * Exception codes for fish catalogue use cases.
 * Part of 002-fish-stock-catalogue.
 */
public enum FishCatalogueExceptionCodes implements ExceptionCode {

    CATALOGUE_ENTRY_NOT_FOUND(1),
    CATALOGUE_ENTRY_NOT_YOURS(2),
    CATALOGUE_REJECTED_READ_ONLY(3),
    CATALOGUE_ADMIN_REQUIRED(4);

    private final int errorCode;

    FishCatalogueExceptionCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }
}
