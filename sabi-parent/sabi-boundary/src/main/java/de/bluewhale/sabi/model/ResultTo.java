package de.bluewhale.sabi.model;

import de.bluewhale.sabi.exception.Message;

/**
 * Used to return an Object along with a success or error message according to its usage.
 *
 * @author schubert
 */
public class ResultTo<T> {

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
