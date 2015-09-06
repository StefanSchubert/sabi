package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.stereotype.Component;

import javax.persistence.Query;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 06.09.15
 */
@Component("userDao")
public class UserDaoImpl extends GenericDaoImpl<UserEntity> implements UserDao {

    public UserEntity loadUserByEmail(String email) {
        Query query = this.em.createQuery("select u FROM UserEntity u where u.email = :email");
        query.setParameter("email", email);
        List<UserEntity> users = query.getResultList();
        if (users != null && users.size() == 1) {
            return users.get(0);
        }
        return null;
    }
}
