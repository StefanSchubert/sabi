package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;

/**
 * Specifies CRUD internal.services required for UserManagement.
 */
public interface UserService {

  /*  *//**
     * LogMessageInfo annotation definition.
     * <p/>
     * message: The message to log.
     * comment: A comment which appears above the message in the
     * LogMessages.properties file.  Useful for localization.
     * level:   The log level.  (default: INFO)
     * cause:   Describes what caused this message to be generated.
     * action:  Describes what the user/admin can do to resolve the problem.
     * publish: Boolean value indicates whether this log message should be
     * published in the Error Reference guide. (default: true)
     * <p/>
     * Example:
     *
     * @LogMessageInfo( message = "This is the log message to be localized.",
     * comment = "This is a comment about the above message.",
     * level = "WARNING",
     * cause = "This describes the cause of the problem...",
     * action = "This describes the action to fix the problem...",
     * publish = false)
     * private static final String EJB005 = "AS-EJB-00005";
     *//*
    @LogMessageInfo(
            message = "User {0} added.",
            comment = "Successfully created the User.",
            level = "INFO")*/
    public static final String USER_CREATED = "Sabi-UserMgmt-000";

/*    @LogMessageInfo(
            message = "User {0} does not exists.",
            comment = "Request on unknown user. Username forgotten?.",
            level = "INFO")*/
    public static final String USER_UNKNOWN = "Sabi-UserMgmt-001";



    /**
     * Creates a new User. The returned object contains the validation Token,
     * which will be used with the registration email.
     */
    public UserTo addUser(UserTo newUser);

}
