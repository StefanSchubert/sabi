/*
 * Copyright (c) 2016 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

/**
 *
 * Author: Stefan Schubert
 * Date: 06.09.15
 */
@Transactional
public interface UserDao extends GenericDao<UserEntity> {

    /**
     * Returns an User object that matches the email given
     *
     * @param email
     * @return
     */
    UserTo loadUserByEmail(@NotNull String email);


    /**
     * creates a new user
     * @param pNewUser
     * @param pPassword
     * @return
     * @throws BusinessException if user already exists
     */
    UserTo create(@NotNull UserTo pNewUser, final String pPassword) throws BusinessException;


    /**
     * Physically deletes the user
     * @param pEmail
     */
    void deleteByEmail(@NotNull String pEmail);


    /**
     * Sets Users valid flag.
     * @param pEmail
     * @param isValidated
     * @throws BusinessException if User is unknown
     */
    void toggleValidationFlag(@NotNull String pEmail, boolean isValidated) throws BusinessException;
}
