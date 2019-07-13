/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.Assert.fail;


/**
 * Business-Layer tests for CoralServices. Requires a running database.
 * User: Stefan
 * Date: 16.06.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    @Ignore
    public void testAddCoral() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, TESTUSER_EMAIL1);

        // When
        fail("Implement me");
    }



}
