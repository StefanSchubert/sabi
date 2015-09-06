package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.UserEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan Schubert
 * Date: 29.08.15
 */
public class UserServiceImpl extends CommonService implements UserService {

    @PersistenceContext(unitName = "sabi")
    private EntityManager em;


    public UserTo addUser(UserTo newUser) {
        UserEntity userEntity = new UserEntity();

        // TODO StS 29.08.15: Using Dozer Mapper?
        userEntity.setEmail(userEntity.getEmail());
        userEntity.setPassword(userEntity.getPassword());

        String validateToken = generateValidationToken();
        userEntity.setValidateToken(validateToken);
        userEntity.setValidated(false);

        newUser.setValidateToken(validateToken);

        // TODO StS 29.08.15: Orchestrating Service should send the email delivering the token.
        em.persist(userEntity);
        return newUser;
    }

    private String generateValidationToken() {
        // TODO StS 29.08.15: random generation
        return "Seagrass123";
    }

}
