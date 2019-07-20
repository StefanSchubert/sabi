/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.persistence.MeasurementRepositoryTest;
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
        MeasurementRepositoryTest.class,
        MeasurementServiceTest.class,
        MeasurementControllerTest.class
})
public class MeasurementTestSuite {

}
