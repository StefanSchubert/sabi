/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;


/**
 * Only the interface in the boundary. The specific codes are thought to be provided by each
 * backend service.
 *
 * @author schubert
 */
public interface ExceptionCode {

    /**
     *
     * @return unique code of occured error for further reference (logging etc..)
     */
    int getErrorCode();

}
