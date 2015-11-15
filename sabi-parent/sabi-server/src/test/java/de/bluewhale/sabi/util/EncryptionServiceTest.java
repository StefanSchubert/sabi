package de.bluewhale.sabi.util;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.util.EncryptionService.AccessToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.*;

/**
 * Tests the encryption service.
 *
 * @author schubert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class EncryptionServiceTest {

    @Autowired
    EncryptionService encryptionService;


    @Test
    public void testGetEncryptedAccessTokenForUser() throws Exception {
        // Given
        String myUserId = "Tünnes@Kölle";

        // When
        String accessTokenForUser = this.encryptionService.getEncryptedAccessTokenForUser(myUserId, null);

        // Then
        assertFalse("Non encryption!", accessTokenForUser.contains(myUserId));
    }


    @Test
    public void testDecryptAccessToken() throws Exception {
        // Given
        String myUserId = "Zaphod.Beeblebrox@beteigeuze.outer.space";
        String encryptedToken = this.encryptionService.getEncryptedAccessTokenForUser(myUserId, null);

        // When
        AccessToken decryptedToken = this.encryptionService.decryptAccessToken(encryptedToken);

        // Then
        assertTrue("Excepted a valid Token", decryptedToken.isValid());
        assertEquals("User decription failed!", decryptedToken.getUserIdentifier(), myUserId);
    }


    @Test
    public void testInvalidAccessToken() throws Exception {
        // Given
        String myUserId = "Zaphod.Beeblebrox@beteigeuze.outer.space";
        long ONE_SECOND = 1;
        String encryptedToken = this.encryptionService.getEncryptedAccessTokenForUser(myUserId, ONE_SECOND);

        // When
        Thread.sleep(1000);
        EncryptionService.AccessToken accessToken = this.encryptionService.decryptAccessToken(encryptedToken);

        // Then
        assertFalse("Token had a 1 sec TTL. Must be expired by now.", accessToken.isValid());

    }
}