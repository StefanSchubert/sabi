/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.persistence.MeasurementDAOTest;
import de.bluewhale.sabi.services.MeasurementServiceTest;
import de.bluewhale.sabi.services.rest.MeasurementControllerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Suite to test the Measurement functionality.
 *
 * @author schubert
 */
@SpringBootTest
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MeasurementDAOTest.class,
        MeasurementServiceTest.class,
        MeasurementControllerTest.class
})
public class MeasurementTestSuite {

}
