/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * Message codes for invertebrate catalogue management use cases.
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
public enum InvertebrateCatalogueMessageCodes implements MessageCode {

    CATALOGUE_PROPOSED,
    CATALOGUE_UPDATED,
    CATALOGUE_APPROVED,
    CATALOGUE_REJECTED,
    // Error codes
    UNKNOWN_USER(InvertebrateCatalogueExceptionCodes.UNKNOWN_USER),
    NOT_YOUR_ENTRY(InvertebrateCatalogueExceptionCodes.NOT_YOUR_ENTRY),
    CATALOGUE_NOT_FOUND(InvertebrateCatalogueExceptionCodes.CATALOGUE_NOT_FOUND),
    NOT_ADMIN(InvertebrateCatalogueExceptionCodes.NOT_ADMIN),
    READ_ONLY_REJECTED(InvertebrateCatalogueExceptionCodes.READ_ONLY_REJECTED),
    CATALOGUE_NAME_DUPLICATE(InvertebrateCatalogueExceptionCodes.CATALOGUE_NAME_DUPLICATE);

    private InvertebrateCatalogueExceptionCodes exceptionCode;

    InvertebrateCatalogueMessageCodes() {
        this.exceptionCode = null;
    }

    InvertebrateCatalogueMessageCodes(InvertebrateCatalogueExceptionCodes exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
