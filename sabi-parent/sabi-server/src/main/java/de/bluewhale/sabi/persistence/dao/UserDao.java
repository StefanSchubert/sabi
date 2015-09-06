package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.dao.GenericDao;
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

}
