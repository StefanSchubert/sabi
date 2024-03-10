/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.configs.TestContainerVersions;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;


/**
 * Persistence-Layer Test for userDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("IntegrationTest")
@Transactional
@DirtiesContext
// DirtiesContext: Spring context is refreshed after the test class is executed,
// which includes reinitializing the HikariCP datasource (which is defined at spring level, while the testcontainer is not)
public class UserRepositoryTest implements TestContainerVersions {


    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private Flyway flyway;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setUp() {

        // flyway.clean(); // Optional: Clean DB before each single test
        // org.flywaydb.core.api.FlywayException: Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.
        flyway.migrate();

    }

    @AfterAll
    static void cleanup() {
        mariaDBContainer.stop();
    }

    @Test
    void connectionEstablished(){
        assertThat(mariaDBContainer.isCreated());
        assertThat(mariaDBContainer.isRunning());
    }


    @Test
    @Rollback
    public void testProbeTracebleAttributeMappingsHasBeenSet() throws Exception {

        String email = "P_USER1_EMAIL@bluewhale.de";

        // Given
        UserEntity testuser1 = new UserEntity();
        testuser1.setEmail(email);
        testuser1.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser1.setUsername("Tim");
        testuser1.setValidateToken("NO_IDEA");
        testuser1.setValidated(true);
        testuser1.setLanguage("de");
        testuser1.setCountry("DE");
        userRepository.save(testuser1);

        // When
        UserEntity userEntity = userRepository.getByEmail(email);

        // Then
        assertNotNull("Missing Default Testdata", userEntity);
        assertNotNull("Temporal Column CreatedOn not mapped.", userEntity.getCreatedOn());
        assertNotNull("Temporal Column LastmodOn not mapped.", userEntity.getLastmodOn());
    }

    @Test
    @Rollback
    public void testCreateUser() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setUsername("Tester");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4711L);

        // when
        userRepository.save(userEntity);

        // then
        UserEntity foundUserEntity = userRepository.getOne(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }

    @Test
    @Rollback
    public void testModifierAttributesViaGenericDAO() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("ModifierTest@bluewhale.de");
        userEntity.setUsername("Sharky");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4712L);

        UserEntity foundUserEntity = userRepository.saveAndFlush(userEntity);
        final LocalDateTime initalModDate = foundUserEntity.getLastmodOn();

        // when we do an update
        Thread.sleep(1100); // make sure we have a second difference
        foundUserEntity.setValidated(true);
        UserEntity updatedUserEntity = userRepository.saveAndFlush(foundUserEntity);

        // then
        assertNotEquals("LastModDates should differ.",initalModDate,updatedUserEntity.getLastmodOn());

    }

}
