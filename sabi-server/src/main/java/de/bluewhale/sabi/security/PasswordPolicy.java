/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import org.apache.logging.log4j.util.Strings;
import org.passay.*;
import org.springframework.lang.Nullable;

/**
 * Serves to enforce a certain password strength.
 *
 * @author Stefan Schubert
 */
public class PasswordPolicy {

    private static PasswordValidator validator = new PasswordValidator(
            new LengthRule(10, 20),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.LowerCase, 1),
            new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1),

            // define some illegal sequences that will fail when >= 5 chars long
            // alphabetical is of the form 'abcde', numerical is '34567', qwery is 'asdfg'
            // the false parameter indicates that wrapped sequences are allowed; e.g. 'xyzabc'
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),

            // no whitespace
            new WhitespaceRule());

    private PasswordPolicy() {
        // Util Class
    }

    /**
     * Checks the given PasswordPhrase against the following ruleset:
     * <ul>
     *     <li>length between 10 and 20 characters</li>
     *     <li>at least one upper-case character</li>
     *     <li>at least one lower-case character</li>
     *     <li>at least one digit character</li>
     *     <li>at least one symbol (special character)</li>
     *     <li>no illegal sequences >=5 chars / abcde 12345 or qwert etc...</li>
     * </ul>
     * @param pPhrase the password to check against the policy.
     * @return true if it holds, othervise false.
     */
    public static boolean isPasswordValid(@Nullable final String pPhrase) {
        if (Strings.isEmpty(pPhrase)) return false;
        final RuleResult result = validator.validate(new PasswordData(pPhrase));
        return result.isValid();
    }

}