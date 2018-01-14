/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO STS: Add Description here...
 *
 * @author Stefan Schubert
 */
public class ObfuscatorTest {

    /**
     * This test is especially useful if you require to store testdata within the database.
     */
    @Test
    public void testEncryptPasswordForHeavensSake() {
        // Given a password
        String TEST_PWD = "UNHASHED_NONSENCE";

        // When
        String encryptedPWD = Obfuscator.encryptPasswordForHeavensSake(TEST_PWD);

        // Then
        assertNotNull(encryptedPWD);
        assertEquals("Hashsecret changed?", encryptedPWD, "229634923501095cce1e24b651971415");
    }
}