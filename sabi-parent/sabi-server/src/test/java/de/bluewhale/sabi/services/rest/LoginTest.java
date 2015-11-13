package de.bluewhale.sabi.services.rest;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.services.UserService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * CRUD Tests within embedded EJB Container.
 * Requires a running database, as configured by jndi.properties
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
@Ignore
public class LoginTest {


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
    public void testCreateUserViaDAO() throws Exception {

        // given a test user

        // when a valid user sends a login request

        // then we should get a jason in return with an validation token.

        fail("Validation Token required.");

    }

}
