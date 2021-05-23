/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.persistence.UserRepositoryTest;
import de.bluewhale.sabi.security.PasswordPolicyTest;
import de.bluewhale.sabi.services.CoralServiceTest;
import de.bluewhale.sabi.services.FishServiceTest;
import de.bluewhale.sabi.services.UserServiceTest;
import de.bluewhale.sabi.services.rest.UserAuthControllerTest;
import de.bluewhale.sabi.services.rest.UserProfileControllerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Suite to test them all.
 *
 * @author schubert
 */
@SpringBootTest
@RunWith(Suite.class)
@SuiteClasses({UserRepositoryTest.class,
        UserServiceTest.class,
        UserAuthControllerTest.class,
        UserProfileControllerTest.class,
        TankTestSuite.class,
        MeasurementTestSuite.class,
        FishServiceTest.class,
        CoralServiceTest.class,
        PasswordPolicyTest.class
})
public class MasterTestSuite {

}
