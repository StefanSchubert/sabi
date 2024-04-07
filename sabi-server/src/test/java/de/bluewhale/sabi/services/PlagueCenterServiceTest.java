/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.PlagueStatusTo;
import de.bluewhale.sabi.persistence.model.LocalizedPlagueStatusEntity;
import de.bluewhale.sabi.persistence.model.PlagueStatusEntity;
import de.bluewhale.sabi.persistence.repositories.PlagueStatusRepository;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;


/**
 * Business-Layer tests for PlagueCenterService.
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ServiceTest")
public class PlagueCenterServiceTest {
    // ------------------------------ FIELDS ------------------------------

            /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

    @Autowired
    private PlagueCenterService plagueCenterService;

    @MockBean
    private PlagueStatusRepository plagueStatusRepository;

    @Autowired
    private UserService userService;

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);


// -------------------------- OTHER METHODS --------------------------


    @Test
    @Transactional
    public void testListTranslatedPlagueStatus() throws Exception {
        // Given
        PlagueStatusEntity plagueStatusEntity = testDataFactory.getTestPlagueStatusEntity();
        List<LocalizedPlagueStatusEntity> localizedPlagueStatusEntities = testDataFactory.getTestLocalizedPlagueStatusEntities();
        plagueStatusEntity.setLocalizedPlagueStatusEntities(localizedPlagueStatusEntities);

        ArrayList<PlagueStatusEntity> plagueStatusEntities = new ArrayList<PlagueStatusEntity>();
        plagueStatusEntities.add(plagueStatusEntity);

        given(this.plagueStatusRepository.findAll()).willReturn(plagueStatusEntities);

        // When
        List<PlagueStatusTo> plagueStatusTos = plagueCenterService.listAllPlagueStatus("en");

        // Then
        assertNotNull(plagueStatusTos);
        assertEquals("Persisted Testdata?", 1, plagueStatusTos.size());
        assertTrue("Language filter brocken?", plagueStatusTos.get(0).getDescription().equals("Spreading"));
    }

}
