package de.bluewhale.sabi.services;

import java.util.logging.Logger;

/**
 * Global class containing things which have all internal.services in common.
 *
 * @author Stefan Schubert
 */
public abstract class CommonService {
          // FIXME STS Logging
    /**
     * The resourceBundle name to be used for the module's log messages
     * When the code is compiled a source level annotation processor processes the
     * LogMessageInfo annotation. A single resource bundle for all LogMessageInfo
     * annotation definitions within the entire module will be created based on the value
     * of a org.glassfish.logging.annotation.LogMessagesResourceBundle annotation as
     * described here. In the above example, it will be com/foo/foobar/LogMessages.properties.
     * <p/>
     * The above would be added to the file:
     * <p/>
     * .../target/classes/com/foo/foobar/LogMessages.properties
     * <p/>
     * In the annotation example above it is important to note that the value of the String variable passed to the log method will serve two purposes:
     * <p/>
     * It is the key used to look up the string in the resource bundle.
     * It is the message ID that will accompany the log message in the log entry.
     */
    public static final String SHARED_LOGMESSAGE_RESOURCE =
            "de.bluewhale.sabi.services.LogMessages";

    /*
     * Logging HOWTO: https://wikis.oracle.com/display/GlassFish/Logging+Guide
     */
    public static final String SERVICE_LOGGER = "de.bluewhale.sabi.services";

    public static final Logger LOGGER =
            Logger.getLogger(SERVICE_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

}
