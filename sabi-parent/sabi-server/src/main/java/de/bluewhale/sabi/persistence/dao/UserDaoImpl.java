package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.services.AuthMessageCodes;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 06.09.15
 */
@Repository("userDao")
@Transactional
public class UserDaoImpl extends GenericDaoImpl<UserEntity> implements UserDao {

    // FIXME: 13.11.2015 Change type to To using dozer Mapper
    public UserEntity loadUserByEmail(String email) {
        Query query = em.createQuery("select u FROM UserEntity u where u.email = :email");
        query.setParameter("email", email);
        List<UserEntity> users = query.getResultList();
        if (users != null && users.size() == 1) {
            return users.get(0);
        }
        return null;
    }


    @Override
    public UserTo create(final UserTo pNewUser) throws BusinessException {

        final UserEntity existingUser = loadUserByEmail(pNewUser.getEmail());

        if (existingUser != null) {
            throw BusinessException.with(AuthMessageCodes.USER_ALREADY_EXISTS, pNewUser.getEmail());
        }

        // TODO: 13.11.2015 Test if we can use dozer for mapping, i'm not quite sure if eclipselink gets trouble if the contained hibernate-core will be joined through this.
        final UserEntity userEntity = new UserEntity();
        userEntity.setEmail(pNewUser.getEmail());
        userEntity.setPassword(pNewUser.getPassword());
        userEntity.setValidateToken(pNewUser.getValidateToken());
        userEntity.setValidated(false);

        final UserEntity createdUser = create(userEntity);
        // Backward Mapping if we have dozer?
        return pNewUser;
    }


    @Override
    public void deleteByEmail(final String pEmail) {
        final UserEntity userEntity = loadUserByEmail(pEmail);
        if (userEntity != null) {
            delete(userEntity.getId());
        }
    }
}
