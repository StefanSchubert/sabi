/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.BasicDataFactory;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * Just a minimalisitc Blueprint Setup to demontrate the usage
 * of TestContainer in Repository Tests
 *
 * To work with Testcontainers I added the following maven dependencies
 * <pre>
 *        &lt;dependency&gt;
 *             &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *             &lt;artifactId&gt;spring-boot-testcontainers&lt;/artifactId&gt;
 *             &lt;scope&gt;test&lt;/scope&gt;
 *         &lt;/dependency&gt;
 *
 *         &lt;dependency&gt;
 *             &lt;groupId&gt;org.testcontainers&lt;/groupId&gt;
 *             &lt;artifactId&gt;junit-jupiter&lt;/artifactId&gt;
 *             &lt;version&gt;${junit.testcontainer.version}&lt;/version&gt;
 *             &lt;scope&gt;test&lt;/scope&gt;
 *         &lt;/dependency&gt;
 *
 *         &lt;dependency&gt;
 *             &lt;groupId&gt;org.testcontainers&lt;/groupId&gt;
 *             &lt;artifactId&gt;mariadb&lt;/artifactId&gt;
 *             &lt;version&gt;${mariadb.testcontainer.version}&lt;/version&gt;
 *             &lt;scope&gt;test&lt;/scope&gt;
 *         &lt;/dependency&gt;
 *</pre>
 */
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TestContainerSampleTest extends BasicDataFactory {

    @Container
    @ServiceConnection // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:11.3.2");

    @Autowired
    private Flyway flyway;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setUp() {

        // flyway.clean(); // Optional: Clean DB before each single test
        // org.flywaydb.core.api.FlywayException: Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.
        flyway.migrate();
        populateBasicData();
    }

    @Test
    void connectionEstablished(){
        assertThat(mariaDBContainer.isCreated());
        assertThat(mariaDBContainer.isRunning());
    }

    @Test
    void hasStoredUsers() throws Exception {
        List<UserEntity> userEntityList = userRepository.findAll();
        assertFalse(userEntityList.isEmpty());
    }

}