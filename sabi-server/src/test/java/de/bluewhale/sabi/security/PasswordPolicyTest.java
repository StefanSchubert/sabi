/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import org.eclipse.persistence.jpa.jpql.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


/**
 * Checks Password Strength Checker
 *
 * @author Stefan Schubert
 */
@ExtendWith(SpringExtension.class)
public class PasswordPolicyTest {

    @Test
    public void passWordToShort() {
        String pw = "Ab!8";
        Assert.isFalse(PasswordPolicy.isPasswordValid(pw), "Allows to short passwords");
    }

    @Test
    public void weekKeyboardPatterns() {
        String qwertz = "qwertz!1A";
        String noncense = "App12345!";
        Assert.isFalse(PasswordPolicy.isPasswordValid(qwertz), "Allows containing week keyboard patterns: "+qwertz);
        Assert.isFalse(PasswordPolicy.isPasswordValid(noncense), "Allows containing week keyboard patterns: "+noncense);
    }

    @Test
    public void validPassword() {
        String cool = "AllRules!8!applied";
        Assert.isTrue(PasswordPolicy.isPasswordValid(cool), "Expected that this holds the Policy: "+cool);
    }

    @Test
    public void tooLessRulesApplied() {
        String a = "CamelCaseOnly";
        String b = "CamelCaseWith8Diget";
        String c = "8digetand#special!";

        Assert.isFalse(PasswordPolicy.isPasswordValid(a), "Pattern not strong enough: "+a);
        Assert.isFalse(PasswordPolicy.isPasswordValid(b), "Pattern not strong enough: "+b);
        Assert.isFalse(PasswordPolicy.isPasswordValid(c), "Pattern not strong enough: "+c);
    }

}