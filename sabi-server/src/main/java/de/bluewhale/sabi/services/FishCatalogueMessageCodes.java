/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for fish catalogue use cases.
 * Part of 002-fish-stock-catalogue.
 */
public enum FishCatalogueMessageCodes implements MessageCode {

    CATALOGUE_ENTRY_PROPOSED,
    CATALOGUE_ENTRY_APPROVED,
    CATALOGUE_ENTRY_REJECTED,
    CATALOGUE_DUPLICATE_WARNING,
    CATALOGUE_ENTRY_UPDATED,
    // Error codes (wrapping ExceptionCodes)
    CATALOGUE_ENTRY_NOT_FOUND(FishCatalogueExceptionCodes.CATALOGUE_ENTRY_NOT_FOUND),
    CATALOGUE_ENTRY_NOT_YOURS(FishCatalogueExceptionCodes.CATALOGUE_ENTRY_NOT_YOURS),
    CATALOGUE_REJECTED_READ_ONLY(FishCatalogueExceptionCodes.CATALOGUE_REJECTED_READ_ONLY),
    CATALOGUE_ADMIN_REQUIRED(FishCatalogueExceptionCodes.CATALOGUE_ADMIN_REQUIRED);

    private FishCatalogueExceptionCodes exceptionCode;

    FishCatalogueMessageCodes() {
        this.exceptionCode = null;
    }

    FishCatalogueMessageCodes(FishCatalogueExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
