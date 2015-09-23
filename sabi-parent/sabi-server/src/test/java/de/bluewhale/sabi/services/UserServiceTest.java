package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.configs.PersistenceJPAConfig;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.dao.UserDaoImpl;
import de.bluewhale.sabi.persistence.model.UserEntity;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


import javax.naming.Context;
import javax.naming.NamingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * CRUD Tests within embedded EJB Container.
 * Requires a running database, as configured by jndi.properties
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UserServiceTest {

    // @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    @BeforeClass
    public static void init() throws NamingException {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Shutdown the embeddable container

    }

    @Test
    @Transactional
    public void testCreateUserViaDAO() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setPassword("Test123");
        userEntity.setId(4711l);

        // when
        userDao.create(userEntity);

        // then
        UserEntity foundUserEntity = userDao.find(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }

    @Test
    @Ignore
    public void testAddUserViaService() throws Exception {


        UserTo userTo = new UserTo("test@bluewhale.de", "NoPass123");

        UserTo createdUser = userService.createUser(userTo);

        assertNotNull("New user was not issued with a token.", createdUser.getValidateToken());
    }
}
