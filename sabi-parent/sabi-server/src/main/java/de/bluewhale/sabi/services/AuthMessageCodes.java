package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.ExceptionCode;
import de.bluewhale.sabi.exception.MessageCode;

/**
 * MessageCodes that may arise during use cases dealing with authentication or authorization.
 *
 * @author schubert
 */
public enum AuthMessageCodes implements MessageCode {
    WRONG_PASSWORD(AuthExceptionCodes.AUTHENTICATION_FAILED),
    UNKNOWN_USERNAME(AuthExceptionCodes.AUTHENTICATION_FAILED),
    USER_CREATION_SUCCEEDED(),
    USER_ALREADY_EXISTS(AuthExceptionCodes.USER_REGISTRATION_FAILED),
    SIGNIN_SUCCEDED();

// ------------------------------ FIELDS ------------------------------

    private AuthExceptionCodes exceptionCode;

// --------------------------- CONSTRUCTORS ---------------------------

    AuthMessageCodes() {
        exceptionCode = null;
    }

    AuthMessageCodes(AuthExceptionCodes pExceptionCode) {
        exceptionCode = pExceptionCode;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @Override
    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}
