/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Contains some little obfuscation routines, for storing the password or for masquerading restful ids...
 *
 * @author Stefan Schubert
 */
public class Obfuscator {

    public static String encryptPasswordForHeavensSake(final String pPassword) {
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
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
