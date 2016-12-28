/*
 * Copyright (c) 2016. by Stefan Schubert
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.model.UserTo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static de.bluewhale.sabi.TestSuite.TESTUSER_EMAIL;
import static org.junit.Assert.*;


/**
 * Business-Layer tests for TankServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class TankServiceTest {
// ------------------------------ FIELDS ------------------------------



    @Autowired
    TankService tankService;

    @Autowired
    UserService userService;

// -------------------------- OTHER METHODS --------------------------

/*
    @BeforeClass
    public static void init() throws NamingException {
    }
*/

/*    @AfterClass
    public static void tearDownClass() throws Exception {
    }
*/


    @Test
    @Transactional
    public void testRegisterNewTank() throws Exception {
        // Given
        final String clearTextPassword = "NoPass123";
        UserTo userTo = new UserTo(TESTUSER_EMAIL, clearTextPassword);
        ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);
        UserTo registeredUser = userToResultTo.getValue();

        AquariumTo aquariumTo = new AquariumTo();
        aquariumTo.setDescription("Test Tank");
        aquariumTo.setSize(40);
        aquariumTo.setSizeUnit(SizeUnit.LITER);

        // When
        ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser);

        // Then
        assertNotNull("ResultObject must not be empty",aquariumToResultTo);
        AquariumTo aquarium = aquariumToResultTo.getValue();
        assertNotNull("ResultObject had no Aquarium inside!",aquarium);
        assertNotNull("Tank ID was not provided!",aquarium.getId());
        assertEquals("User Assignment missing.",registeredUser.getId(), aquarium.getUserId());
        assertEquals("Wrong message type.",CATEGORY.INFO, aquariumToResultTo.getMessage().getType());

    }


    @Test
    @Transactional
    public void testFetchAllTanks() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    /**
     * Tank properties are something like name, description, size.
     * Excluded are inhabitants etc... they are linked to a tank
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testAlterTankProperties() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    /**
     * Add a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testAddFish() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    /**
     * Remode a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testRemoveFish() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    /**
     * Yes, someone may write a kind of blog
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testAddLogEntry() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    @Test
    @Transactional
    public void testAddCoral() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


    @Test
    @Transactional
    public void testRemoveCoral() throws Exception {
        // Given

        // When

        // Then
        fail("Complete implementation needed.");
    }


}
