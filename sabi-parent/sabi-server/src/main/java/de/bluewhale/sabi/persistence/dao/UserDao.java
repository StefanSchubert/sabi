package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.persistence.model.UserEntity;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 06.09.15
 */
public interface UserDao extends GenericDao<UserEntity> {

    /**
     * Returns an User object that matches the email given
     *
     * @param email
     * @return
     */
    public UserEntity loadUserByEmail(String email);


    /**
     * creates a new user
     * @param pNewUser
     * @return
     * @throws BusinessException if user already exists
     */
    UserTo create(UserTo pNewUser) throws BusinessException;


    /**
     * Physically deletes the user
     * @param pEmail
     */
    void deleteByEmail(String pEmail);
}
