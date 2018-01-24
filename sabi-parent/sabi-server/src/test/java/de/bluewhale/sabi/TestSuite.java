/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.persistence.MeasurementDAOTest;
import de.bluewhale.sabi.persistence.UserDAOTest;
import de.bluewhale.sabi.services.CoralServiceTest;
import de.bluewhale.sabi.services.FishServiceTest;
import de.bluewhale.sabi.services.TankServiceTest;
import de.bluewhale.sabi.services.UserServiceTest;
import de.bluewhale.sabi.services.rest.UserAuthTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Suite to test them all.
 *
 * @author schubert
 */
@SpringBootTest
@RunWith(Suite.class)
@Suite.SuiteClasses({UserDAOTest.class,
        MeasurementDAOTest.class,
        UserServiceTest.class,
        TankServiceTest.class,
        FishServiceTest.class,
        CoralServiceTest.class,
        UserAuthTest.class
})
public class TestSuite {


}
