package de.bluewhale.sabi.util;

import com.sun.istack.internal.Nullable;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.SaltGenerator;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encodes and decodes access tokens.
 *
 * @author schubert
 */
public class EncryptionService {
// ------------------------------ FIELDS ------------------------------

    private static int DATE_TIME_LENGTH = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()).length();

    StandardPBEStringEncryptor encryptor;

    SaltGenerator saltGenerator;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Object dateFormatLocker = new Object();

    ;

    @Value("${accessToken.TTL}")
    private long accessTokenMaxValidityPeriodInSecs;

// --------------------------- CONSTRUCTORS ---------------------------

    public EncryptionService(String salt, String password) {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setSaltGenerator(saltGenerator = new org.jasypt.salt.StringFixedSaltGenerator(salt));
        encryptor.setPassword(password);
    }

// -------------------------- OTHER METHODS --------------------------


    /**
     * Issues an access Token for the User
     *
     * @param pUserIdentifier       this may be a technical of business key of your user
     * @param pValidPeriodInSeconds if provided, this will be used to calculate the validation period.
     *                              If null, we take the value from the server.properties (key: accessToken.TTL).
     * @return An encrypted AccessToken.
     */
    public String getEncryptedAccessTokenForUser(@NotNull String pUserIdentifier, @Nullable final Long pValidPeriodInSeconds) {
        // calculate validity period (1000 milliseconds per second)
        Date now = new Date();

        now.setTime(now.getTime() + (1000 * (pValidPeriodInSeconds == null ? accessTokenMaxValidityPeriodInSecs :
                                             pValidPeriodInSeconds)));
        String date;

        // Lock DateFormatter, since it is not thread-safe.
        synchronized (dateFormatLocker) {
            date = dateFormatter.format(now);
        }
        String accessToken = encryptor.encrypt(date + pUserIdentifier);

        try {
            decryptAccessToken(accessToken);
        }
        catch (org.jasypt.exceptions.EncryptionOperationNotPossibleException e) {
            // TODO: 14.11.2015 add max recursive constraint and do a better logging
            System.out.println("Generated broken cypher (" + accessToken + ")");
            e.printStackTrace();
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e1) {
                // TODO: 14.11.2015 add max recursive constraint and do a better logging
                System.out.println("Could not generate new cypher!"); //
                e1.printStackTrace();
            }
            accessToken = getEncryptedAccessTokenForUser(pUserIdentifier, pValidPeriodInSeconds); // recursive loop!
        }

        return accessToken;
    }


    public AccessToken decryptAccessToken(String pCipher) {
        String message = null;
        try {
            message = encryptor.decrypt(pCipher);
        }
        catch (org.jasypt.exceptions.EncryptionOperationNotPossibleException e) {
            System.out.println("### Could not decrypt Cipher(" + pCipher + ")");
            e.printStackTrace();
            return null;
        }
        String userName = message.substring(DATE_TIME_LENGTH, message.length());

        Date date;
        try {
            // Lock DateFormatter, since it is not thread-safe.
            synchronized (dateFormatLocker) {
                date = dateFormatter.parse(message.substring(0, DATE_TIME_LENGTH));
            }
        }
        catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // TODO: Maybe return a token, but with an expiration date that is in the past?!
            return null;
        }

        return new AccessToken(userName, date);
    }

// -------------------------- INNER CLASSES --------------------------


    public class AccessToken {
        public AccessToken(String pUserIdentifier, Date pExpirationDate) {
            userIdentifier = pUserIdentifier;
            expirationDate = pExpirationDate;
        }


        private String userIdentifier;

        private Date expirationDate;


        public String getUserIdentifier() {
            return userIdentifier;
        }


        public Date getExpirationDate() {
            return expirationDate;
        }


        public boolean isValid() {
            Date now = new Date();
            return now.before(expirationDate);
        }
    }
}
