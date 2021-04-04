/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import org.apache.logging.log4j.util.Strings;

/**
 * Takes care of enforcing some password policy
 *
 * @author Stefan Schubert
 */
public class PasswordPolicy {

    /**
     * Checks if we have a policy violation
     * @param password
     * @param checkPwd should be the same password
     * @return true if passwords are not identical, or to week
     */
    public static boolean failedCheck(String password, String checkPwd) {
        boolean result = false;

        if (Strings.isBlank(password) || Strings.isBlank(checkPwd)) return true;
        if (!password.equals(checkPwd)) return true;
        if (password.length() < 8) return true;
        // TODO STS (04.04.21): Implement some smart password policy check - like min. letter sign Character etc...

        return result;
    }


}
