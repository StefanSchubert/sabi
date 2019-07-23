/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.model.UserTo;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.Assert.*;


/**
 * Business-Layer tests for TankServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TankServiceTest {
    private static final String JABA_DABA_DOOOOO = "JabaDabaDooooo";
    // ------------------------------ FIELDS ------------------------------

    @Autowired
    private TankService tankService;

    @Autowired
    private UserService userService;

// -------------------------- OTHER METHODS --------------------------

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
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getPersistedTestUserTo(TESTUSER_EMAIL1);

        AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser.getEmail());

        // When
        aquariumTo = aquariumToResultTo.getValue();
        aquariumTo.setDescription(JABA_DABA_DOOOOO);

        // Then
        aquariumToResultTo = tankService.updateTank(aquariumTo, TESTUSER_EMAIL1);

        aquariumTo = tankService.getTank(aquariumToResultTo.getValue().getId(), TESTUSER_EMAIL1);

        // Then
        assertEquals("Tank was not updated", aquariumTo.getDescription(), JABA_DABA_DOOOOO);
    }

   @Test
   @Transactional
    public void testListUsersTanks() throws Exception {
        // Given
       TestDataFactory testDataFactory = TestDataFactory.getInstance().withTankService(tankService).withUserService(userService);
       UserTo registeredUser = testDataFactory.getPersistedTestUserTo(TESTUSER_EMAIL1);

        AquariumTo aquariumTo1 = new AquariumTo();
        aquariumTo1.setDescription("Small Test Tank");
        aquariumTo1.setSize(40);
        aquariumTo1.setSizeUnit(SizeUnit.LITER);

        AquariumTo aquariumTo2 = new AquariumTo();
        aquariumTo2.setDescription("Big Test Tank");
        aquariumTo2.setSize(120);
        aquariumTo2.setSizeUnit(SizeUnit.LITER);

       ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo1, registeredUser.getEmail());
       ResultTo<AquariumTo> aquariumToResultTo1 = tankService.registerNewTank(aquariumTo2, registeredUser.getEmail());

       // When
        List<AquariumTo> usersAquariums =  tankService.listTanks(TESTUSER_EMAIL1);

        // Then
        assertNotNull(usersAquariums);
        assertEquals("Persisted Testdata?",2, usersAquariums.size());
        assertTrue(usersAquariums.get(0).getDescription() == aquariumTo1.getDescription());
        assertTrue(usersAquariums.get(1).getDescription() == aquariumTo2.getDescription());
    }

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
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getPersistedTestUserTo(TESTUSER_EMAIL1);

        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();

        // When
        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, TESTUSER_EMAIL1);

        // Then
        assertNotNull("ResultObject must not be empty",aquariumToResultTo);
        final AquariumTo aquarium = aquariumToResultTo.getValue();
        assertNotNull("ResultObject had no Aquarium inside!",aquarium);
        assertNotNull("Tank ID was not provided!",aquarium.getId());
        assertEquals("User Assignment missing.",registeredUser.getId(), aquarium.getUserId());
        assertEquals("Wrong message type.", CATEGORY.INFO, aquariumToResultTo.getMessage().getType());
    }


    @Test
    @Transactional
    public void testRemoveTank() throws Exception {

        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getPersistedTestUserTo(TESTUSER_EMAIL1);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser.getEmail());

        Long persistedTankId = aquariumToResultTo.getValue().getId();
        tankService.removeTank(persistedTankId, TESTUSER_EMAIL1);

        // When
        AquariumTo tankAfterDeletion = tankService.getTank(persistedTankId, TESTUSER_EMAIL1);

        // Then
        assertNull("Users tank was supposed to be removed!", tankAfterDeletion);

    }

}
