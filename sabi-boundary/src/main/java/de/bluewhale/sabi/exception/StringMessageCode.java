/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

/**
 * Generic string-based implementation of {@link MessageCode} used for JSON deserialization
 * when the concrete enum type (e.g. FishStockMessageCodes) is not available on the classpath.
 *
 * The backend serializes MessageCode enum values as their name string. The webclient
 * deserializes them into this wrapper so Jackson does not fail on an abstract type.
 *
 * @author schubert
 */
public class StringMessageCode implements MessageCode {

    private final String name;

    public StringMessageCode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public ExceptionCode getExceptionCode() {
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}

