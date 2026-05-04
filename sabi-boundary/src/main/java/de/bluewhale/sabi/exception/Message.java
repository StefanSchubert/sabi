/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.exception;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 * Used to exchange messages between frontend and backend.
 *
 * @author schubert
 */
public class Message implements Serializable {
// ------------------------------ FIELDS ------------------------------

    @JsonDeserialize(using = MessageCodeDeserializer.class)
    private MessageCode code;

    private CATEGORY type;

    private Serializable[] args;

// -------------------------- STATIC METHODS --------------------------

    public static Message info(MessageCode pMessageCode, Serializable... args) {
        return new Message(pMessageCode, CATEGORY.INFO, args);
    }

    public static Message warning(MessageCode pMessageCode, Serializable... args) {
        return new Message(pMessageCode, CATEGORY.WARNING, args);
    }

    public static Message error(MessageCode pMessageCode, Serializable... args) {
        return new Message(pMessageCode, CATEGORY.ERROR, args);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /** No-arg constructor required for Jackson deserialization. */
    public Message() {
    }

    public Message(MessageCode pCode, CATEGORY pType, Serializable... pArgs) {
        code = pCode;
        type = pType;
        args = pArgs;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Object[] getArgs() {
        return this.args;
    }

    public void setArgs(Serializable[] args) {
        this.args = args;
    }

    public MessageCode getCode() {
        return this.code;
    }

    public void setCode(MessageCode code) {
        this.code = code;
    }

    public CATEGORY getType() {
        return this.type;
    }

    public void setType(CATEGORY type) {
        this.type = type;
    }

// -------------------------- ENUMERATIONS --------------------------

    public enum CATEGORY {
        INFO,
        WARNING,
        ERROR
    }
}
