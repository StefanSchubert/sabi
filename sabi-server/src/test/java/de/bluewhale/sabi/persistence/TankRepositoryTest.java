/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestContainerVersions;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.AssertionErrors.assertTrue;


/**
 * Persistence-Layer Test for AquariumRepository
 * User: Stefan
 * Date: 3.3.2021
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
public class TankRepositoryTest implements TestContainerVersions {

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private Flyway flyway;

    @Autowired
    AquariumRepository aquariumRepository;

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
    public void testFindAllTanksOfSpecificUserById() throws Exception {

        // given through Flyway i.g. basic test data
        String testUser = "sabi@bluewhale.de";
        UserEntity storedUser = userRepository.getByEmail(testUser);

        // when
        List<AquariumEntity> tanksOfUser1 = aquariumRepository.findAllByUser_IdIs(storedUser.getId());

        // then
        assertTrue("Predefined BasicUser one should have exactly 2 tanks", tanksOfUser1.size() == 2);
    }


}
