/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.services.TankServiceTest;
import de.bluewhale.sabi.services.rest.TankControllerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Suite to test the tank functionality.
 *
 * @author schubert
 */
@SpringBootTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TankServiceTest.class,
        TankControllerTest.class
})
public class TankTestSuite {

}
