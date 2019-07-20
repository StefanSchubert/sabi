/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

import java.io.Serializable;

/**
 * Only the interface in the boundary. The specific codes are thought to be provided by each
 * backend service.
 *
 * @author schubert
 */
public interface MessageCode extends Serializable {

    /**
     * Provides an exception code if the MessageCode belongs to it.
     * @return null if there is no such link.
     */
    ExceptionCode getExceptionCode();
}
