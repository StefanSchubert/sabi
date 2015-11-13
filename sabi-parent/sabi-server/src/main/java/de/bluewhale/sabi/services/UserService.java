package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;

/**
 * Provides all required services for user management use cases.
 */
public interface UserService {

    /**
     * Creates a new User. The returned object contains the created
     * user with a message. The user has been created successfully
     * only if the message is of {@link CATEGORY#INFO}
     */
    ResultTo<UserTo> registerNewUser(UserTo newUser);


    /**
     * Drops the user and all of his not commonly shared data.
     * @param pEmail
     */
    void unregisterUserAndClearPersonalData(String pEmail);
}
