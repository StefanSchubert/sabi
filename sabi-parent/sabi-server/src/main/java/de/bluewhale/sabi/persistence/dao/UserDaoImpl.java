package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.services.AuthMessageCodes;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 * Author: Stefan Schubert
 * Date: 06.09.15
 */
@Repository("userDao")
public class UserDaoImpl extends GenericDaoImpl<UserEntity> implements UserDao {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface UserDao ---------------------

    public UserTo loadUserByEmail(@NotNull String email) {
        Query query = em.createQuery("select u FROM UserEntity u where u.email = :email");
        query.setParameter("email", email);
        List<UserEntity> users = query.getResultList();
        if (users != null && users.size() == 1) {
            return mapEntity2To(users.get(0));
        }
        return null;
    }

    @Override
    public UserTo create(@NotNull final UserTo pNewUser, final String pPassword) throws BusinessException {
        final UserTo existingUser = loadUserByEmail(pNewUser.getEmail());

        if (existingUser != null) {
            throw BusinessException.with(AuthMessageCodes.USER_ALREADY_EXISTS, pNewUser.getEmail());
        }

        // TODO: 13.11.2015 Test if we can use dozer for mapping, i'm not quite sure if eclipselink gets trouble if the contained hibernate-core will be joined through this.
        final UserEntity userEntity = new UserEntity();
        userEntity.setEmail(pNewUser.getEmail());
        userEntity.setPassword(pPassword);
        userEntity.setValidateToken(pNewUser.getValidateToken());
        userEntity.setValidated(false);

        final UserEntity createdUser = create(userEntity);
        // Backward Mapping if we have dozer?
        pNewUser.setId(userEntity.getId());
        pNewUser.setPassword(userEntity.getPassword());
        return pNewUser;
    }

    @Override
    public void deleteByEmail(@NotNull final String pEmail) {
        final UserTo userTo = loadUserByEmail(pEmail);
        if (userTo != null) {
            delete(userTo.getId());
        }
    }

    @Override
    public void toggleValidationFlag(@NotNull final String pEmail, final boolean isValidated) throws BusinessException {
        if (pEmail != null) {
            final UserEntity userEntity = getUserByEmail(pEmail);
            userEntity.setValidated(isValidated);
            update(userEntity);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    private UserEntity getUserByEmail(@NotNull String email) {
        Query query = em.createQuery("select u FROM UserEntity u where u.email = :email");
        query.setParameter("email", email);
        List<UserEntity> users = query.getResultList();
        if (users != null && users.size() == 1) {
            return users.get(0);
        }
        return null;
    }

    private UserTo mapEntity2To(@NotNull final UserEntity pUserEntity) {
        // TODO: 14.11.2015 Introduce Dozer? If its transitive hibernate-core dependency makes no trouble with eclipslink. 
        final UserTo userTo = new UserTo(pUserEntity.getEmail(), pUserEntity.getPassword());
        userTo.setValidateToken(pUserEntity.getValidateToken());
        userTo.setValidated(pUserEntity.isValidated());
        userTo.setId(pUserEntity.getId());
        return userTo;
    }
}
