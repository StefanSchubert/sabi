/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.exception;

import java.io.Serializable;

/**
 * Used to exchange messages between frontend and backend.
 *
 * @author schubert
 */
public class Message implements Serializable {
// ------------------------------ FIELDS ------------------------------

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

    public Message(MessageCode pCode, CATEGORY pType, Serializable... pArgs) {
        code = pCode;
        type = pType;
        args = pArgs;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Object[] getArgs() {
        return this.args;
    }

    public MessageCode getCode() {
        return this.code;
    }

    public CATEGORY getType() {
        return this.type;
    }

// -------------------------- ENUMERATIONS --------------------------

    public enum CATEGORY {
        INFO,
        WARNING,
        ERROR
    }
}
