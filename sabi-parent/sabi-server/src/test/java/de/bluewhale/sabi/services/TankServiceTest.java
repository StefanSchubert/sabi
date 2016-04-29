package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.fail;


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

    private static final String TESTUSER_EMAIL = "testservice@bluewhale.de";

    @Autowired
    TankService tankService;

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

        // When

        // Then
        fail("Complete implementation needed.");
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
