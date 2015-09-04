package de.bluewhale.sabi.services;

import de.bluewhale.sabi.model.UserTo;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.NamingException;


/**
 * CRUD Tests within embedded EJB Container.
 * Requires a running database, as configured by jndi.properties
 * User: Stefan
 * Date: 30.08.15
 */
public class UserServiceTest {

    private static Context ctx;
    UserService userService;

    @BeforeClass
    public static void init() throws NamingException {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Shutdown the embeddable container

    }

    @Test
    public void testAddUser() throws Exception {

        // Retrieve a reference to the session bean using a portable
        // global JNDI name
        userService = (UserService)
                ctx.lookup("java:global/classes/UserService");


        UserTo userTo = new UserTo("test@bluewhale.de", "NoPass123");

        UserTo createdUser = userService.addUser(userTo);

        Assert.assertNotNull("New user was not issued with a token.", createdUser.getValidateToken());
    }
}
