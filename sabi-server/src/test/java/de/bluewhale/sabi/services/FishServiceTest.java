/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.Assert.*;


/**
 * Business-Layer tests for TankServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishServiceTest {
// ------------------------------ FIELDS ------------------------------

    @Autowired
    private TankService tankService;
    @Autowired
    private FishService fishService;
    @Autowired
    private UserService userService;

// -------------------------- OTHER METHODS --------------------------

    /**
     * Add a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testAddFish() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getRegisterNewTestUser(TESTUSER_EMAIL1);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();

        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser.getEmail());


        // When
        final FishTo fish = new FishTo();
        fish.setAddedOn(LocalDate.now());
        fish.setFishCatalogueId(1L); // existing default Data
        fish.setAquariumId(aquariumToResultTo.getValue().getId());
        fish.setNickname("Green Latern");

        // The user is required to check that he or she really possesses the tank
        final ResultTo<FishTo> fishResultTo = fishService.registerNewFish(fish,registeredUser);

        // Then
        assertNotNull("ResultObject must not be empty",fishResultTo);
        final FishTo persistedFish = fishResultTo.getValue();
        assertNotNull("ResultObject had no Fish inside!",persistedFish);
        assertNotNull("Tank ID was not provided!",persistedFish.getAquariumId());
        assertEquals("Wrong message type.", CATEGORY.INFO, fishResultTo.getMessage().getType());
    }


    // A User cannot register a fish for a tank that he or she does not own.
    @Test
    @Transactional
        public void testAddFishForWrongTank() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getRegisterNewTestUser(TESTUSER_EMAIL1);
        final UserTo fraudUser = testDataFactory.getRegisterNewTestUser("I_Intent@No.good");
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser.getEmail());


        // When
        final FishTo fish = new FishTo();
        fish.setAddedOn(LocalDate.now());
        fish.setFishCatalogueId(1L); // existing default Data
        fish.setAquariumId(aquariumToResultTo.getValue().getId());
        fish.setNickname("Green Latern");

        // The the fraud user tries to place something in a different tank
        final ResultTo<FishTo> fishResultTo = fishService.registerNewFish(fish,fraudUser);

        // then
        assertNull("ResultObject Value should be empty as creation was not permitted.",fishResultTo.getValue());
        assertEquals("Wrong message type.", CATEGORY.ERROR, fishResultTo.getMessage().getType());

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


    /**
     * Remode a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Transactional
    public void testRemoveFish() throws Exception {
        // Given
        TestDataFactory testDataFactory = TestDataFactory.getInstance().withUserService(userService);
        final UserTo registeredUser = testDataFactory.getRegisterNewTestUser(TESTUSER_EMAIL1);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();

        final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, registeredUser.getEmail());

        final FishTo fish = new FishTo();
        fish.setAddedOn(LocalDate.now());
        fish.setFishCatalogueId(1L); // existing default Data
        fish.setAquariumId(aquariumToResultTo.getValue().getId());
        fish.setNickname("Green Latern");

        // The user is required to check that he your she really posses the tank
        final ResultTo<FishTo> fishResultTo = fishService.registerNewFish(fish,registeredUser);

        // When
        Long fishId = fishResultTo.getValue().getId();
        fishService.removeFish(fishId, registeredUser);

        // Then
        FishTo removedFish = fishService.getUsersFish(fishId, registeredUser);
        assertNull("Fish was not removed!", removedFish);
    }
}
