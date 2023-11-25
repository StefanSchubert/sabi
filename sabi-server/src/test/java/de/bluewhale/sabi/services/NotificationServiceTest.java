/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.test.util.AssertionErrors.fail;


/**
 * Just to test mail connectivity
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Test
    @Disabled
    // To avoid mail spam, this test with real mail server settings was just to see
    // that the real mailserver handshake works.
    public void sendWelcomeMail() {

        try {
            notificationService.sendWelcomeMail("yourmail@somewhere.fast");
        } catch (MessagingException e) {
            fail("Could not reach mail server. Reason: "+e.toString());
        }

    }
}