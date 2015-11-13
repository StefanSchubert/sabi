package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.persistence.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan Schubert
 * Date: 29.08.15
 */
@Service
public class UserServiceImpl extends CommonService implements UserService {

    @Autowired
    private UserDao dao;

    public ResultTo<UserTo> registerNewUser(UserTo newUser) {

        String validateToken = generateValidationToken();
        newUser.setValidateToken(validateToken);
        newUser.setValidated(false);

        UserTo createdUser = null;
        Message message;
        try {
            createdUser = dao.create(newUser);
            message = Message.info(AuthMessageCodes.USER_CREATION_SUCCEEDED, createdUser.getEmail());
            // TODO StS 29.08.15: Orchestrating Service should send the email delivering the token.
        }
        catch (BusinessException pE) {
            message = Message.error(AuthMessageCodes.USER_ALREADY_EXISTS, newUser.getEmail());
        }

        final ResultTo<UserTo> userToResultTo = new ResultTo<>(createdUser, message);
        return userToResultTo;
    }

    private String generateValidationToken() {
        // TODO StS 29.08.15: random generation
        return "Seagrass123";
    }


    @Override
    public void unregisterUserAndClearPersonalData(final String pEmail) {
        // Hook in here to delete other personal data
        dao.deleteByEmail(pEmail);
    }
}
