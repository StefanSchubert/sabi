package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.persistence.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan Schubert
 * Date: 29.08.15
 */
@Service
public class UserServiceImpl extends CommonService implements UserService {

    private static final int DEFAULT_ACCESSTOKEN_TTL_IN_SECONDS = 18000;

    @Autowired
    private UserDao dao;

    public ResultTo<UserTo> registerNewUser(@NotNull  UserTo newUser) {

        String validateToken = generateValidationToken();
        newUser.setValidateToken(validateToken);
        newUser.setValidated(false);

        UserTo createdUser = null;
        Message message;
        try {
            String encryptedPassword = encryptPasswordForHeavensSake(newUser.getPassword());
            createdUser = dao.create(newUser, encryptedPassword);
            message = Message.info(AuthMessageCodes.USER_CREATION_SUCCEEDED, createdUser.getEmail());
            // TODO StS 29.08.15: Orchestrating Service should send the email delivering the token.
        }
        catch (BusinessException pE) {
            message = Message.error(AuthMessageCodes.USER_ALREADY_EXISTS, newUser.getEmail());
        }

        final ResultTo<UserTo> userToResultTo = new ResultTo<>(createdUser, message);
        return userToResultTo;
    }


    private String encryptPasswordForHeavensSake(final String pPassword) {
        // using MD5
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(pPassword.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    private String generateValidationToken() {
        // TODO StS 29.08.15: random generation
        return "Seagrass123";
    }


    @Override
    public void unregisterUserAndClearPersonalData(@NotNull String pEmail) {
        // Hook in here to delete other personal data
        dao.deleteByEmail(pEmail);
    }


    @Override
    public boolean validateUser(@NotNull final String pEmail, @NotNull final String pToken) {
        boolean result = false;
        if (pEmail != null && pToken != null) {
            final UserTo userTo = dao.loadUserByEmail(pEmail);
            if (pToken.equals(userTo.getValidateToken())) {
                try {
                    dao.toggleValidationFlag(pEmail, true);
                    result = true;
                }
                catch (BusinessException pE) {
                    result = false;
                }
            }
        }
        return result;
    }


    @Override
    public ResultTo<String> signIn(@NotNull final String pEmail, @NotNull final String pClearTextPassword) {
        final String password = encryptPasswordForHeavensSake(pClearTextPassword);
        final UserTo userTo = dao.loadUserByEmail(pEmail);
        if (userTo != null) {
            if (userTo.getPassword().equals(password)) {
                String accessToken = generateAccessToken(pEmail, DEFAULT_ACCESSTOKEN_TTL_IN_SECONDS);
                final Message successMessage = Message.info(AuthMessageCodes.SIGNIN_SUCCEDED, pEmail);
                return new ResultTo<String>(accessToken, successMessage);
            } else {
                final Message errorMsg = Message.error(AuthMessageCodes.WRONG_PASSWORD, pEmail);
                return new ResultTo<String>(null, errorMsg);
            }
            
            
        } else {
            final Message errorMsg = Message.error(AuthMessageCodes.UNKNOWN_USERNAME, pEmail);
            return new ResultTo<String>(null, errorMsg);
        }
        
    }


    private String generateAccessToken(final String pEmail, final int pTTLInSeconds) {
        // FIXME: 14.11.2015 Do something sophisticated
        return "It's me pal";
    }
}
