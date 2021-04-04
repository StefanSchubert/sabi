/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.BasicDataFactory;
import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;


/**
 * Persistence-Layer Test for AquariumRepository
 * User: Stefan
 * Date: 3.3.2021
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// @DataJpaTest todo does not work yet missing visible constructor in JPAConfig class - maybe not compatible with eclipse way?
public class TankRepositoryTest extends BasicDataFactory {

    static TestDataFactory testDataFactory;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeClass
    public static void initTestDataFactory() {
        testDataFactory = TestDataFactory.getInstance();
    }

    /**
     * There seems to be a timing problem with H2, that causes, that the basic data is not available
     * for some test classes, while for others it worked out. Until we know what's going wrong...
     * we "double inject" by extending the BasicTestDataFactory and by calling it directly.
     * The different behaviour can be observed by e.g. calling the master test suite and as comparising
     * the measurement testsuite while this is method is deaktivated.
     */
    @Before
    public void ensureBasicDataAvailability() {

        UserEntity byEmail = userRepository.getByEmail(P_USER1_EMAIL);
        if (byEmail == null) populateBasicData();
        UserEntity byEmail2 = userRepository.getByEmail(P_USER1_EMAIL);
        assertNotNull("H2-Basicdata injection did not work!" ,byEmail2);
    }


    @Test
    public void testFindAllTanksOfSpecificUserById() throws Exception {

        // given through BasicDataFactory
        // User 1 has 2 tanks
        // User 2 has 1 tank
        UserEntity user1 = userRepository.getOne(1L);
        UserEntity user2 = userRepository.getOne(2L);

        // when
        List<AquariumEntity> tanksOfUser1 = aquariumRepository.findAllByUser_IdIs(user1.getId());
        List<AquariumEntity> tanksOfUser2 = aquariumRepository.findAllByUser_IdIs(user2.getId());

        // then
        Assert.assertTrue("User1 one should have excatly 2 tanks", tanksOfUser1.size() == 2);
        Assert.assertTrue("User2 one should have excatly 1 tank", tanksOfUser2.size() == 1);
        Assert.assertFalse("Delivered Tank from other user!?", tanksOfUser1.contains(tanksOfUser2));
    }


}
