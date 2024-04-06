/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.bluewhale.sabi.configs.TestContainerVersions.MARIADB_11_3_2;
import static org.springframework.test.util.AssertionErrors.fail;


/**
 * Just to test mail connectivity
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ServiceTest")
public class NotificationServiceTest {

        /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

    @Autowired
    NotificationService notificationService;

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);


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