/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.model;

import de.bluewhale.sabi.exception.Message;

import java.io.Serializable;

/**
 * Used to return an Object along with a success or error message according to its usage.
 *
 * @author schubert
 */
public class ResultTo<T> implements Serializable {

    private T value;
    private Message message;


    public ResultTo(T pValue, Message pMessage) {
        this.value = pValue;
        this.message = pMessage;
    }


    public T getValue() {
        return this.value;
    }


    public Message getMessage() {
        return this.message;
    }
}
