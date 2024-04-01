/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static org.springframework.test.util.AssertionErrors.fail;


/**
 * Business-Layer tests for CoralServices. Requires a running database.
 * User: Stefan
 * Date: 16.06.2017
 */
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CoralServiceTest {
// ------------------------------ FIELDS ------------------------------

    @Autowired
    private TankService tankService;

    @Autowired
    private UserService userService;

// -------------------------- OTHER METHODS --------------------------

    /**
     * Add a coral to users tank.
     *
     * @throws Exception
     */
    @Test
    @Transactional
    @Disabled
    public void testAddCoral() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance();
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, TESTUSER_EMAIL1);

        // When
        fail("Implement me");
    }



}
