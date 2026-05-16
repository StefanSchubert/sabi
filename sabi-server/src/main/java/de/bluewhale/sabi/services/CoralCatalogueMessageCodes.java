/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for coral catalogue management use cases.
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
public enum CoralCatalogueMessageCodes implements MessageCode {

    ENTRY_PROPOSED,
    ENTRY_UPDATED,
    ENTRY_APPROVED,
    ENTRY_REJECTED,
    DUPLICATE_SCIENTIFIC_NAME_WARNING,
    // Error codes
    UNKNOWN_USER(CoralCatalogueExceptionCodes.UNKNOWN_USER),
    NOT_YOUR_ENTRY(CoralCatalogueExceptionCodes.NOT_YOUR_ENTRY),
    ENTRY_NOT_FOUND(CoralCatalogueExceptionCodes.ENTRY_NOT_FOUND),
    NOT_ADMIN(CoralCatalogueExceptionCodes.NOT_ADMIN),
    READ_ONLY_REJECTED(CoralCatalogueExceptionCodes.READ_ONLY_REJECTED);

    private CoralCatalogueExceptionCodes exceptionCode;

    CoralCatalogueMessageCodes() {
        this.exceptionCode = null;
    }

    CoralCatalogueMessageCodes(CoralCatalogueExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
