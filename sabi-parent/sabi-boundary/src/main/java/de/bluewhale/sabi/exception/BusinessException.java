package de.bluewhale.sabi.exception;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Supposed to save lots of different exceptions.
 * Reason is contained in the message object.
 *
 * @author schubert
 */
public class BusinessException extends Exception {
// ------------------------------ FIELDS ------------------------------

    private ExceptionCode code;
    private List<Message> messages = new ArrayList();

// -------------------------- STATIC METHODS --------------------------

    public static BusinessException with(MessageCode pCode, Serializable... args) {
        final Message message = Message.error(pCode, args);
        BusinessException be;

        if (pCode.getExceptionCode() != null) {
            be = new BusinessException(pCode.getExceptionCode(), message);
        } else {
            be = new BusinessException(CommonExceptionCodes.INTERNAL_ERROR, message);
        }
        return be;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public BusinessException(final Message pMessage) {
        messages.add(pMessage);
        code = null;
    }

    public BusinessException(ExceptionCode pCode, Message... pMessages) {
        code = pCode;
        for (Message msg : pMessages) {
            messages.add(msg);
        }
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public ExceptionCode getCode() {
        return code;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
