package de.bluewhale.sabi;

import de.bluewhale.sabi.persistence.UserDAOTest;
import de.bluewhale.sabi.services.UserServiceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Suite to test them all.
 *
 * @author schubert
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({UserDAOTest.class, UserServiceTest.class })
public class TestSuite {
}
