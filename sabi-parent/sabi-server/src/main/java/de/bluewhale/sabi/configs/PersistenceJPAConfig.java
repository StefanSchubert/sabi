package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.dao.UserDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 05.09.15
 */
@Configuration
@EnableTransactionManagement
public class PersistenceJPAConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(new String[]{"de.bluewhale.sabi.persistence.model"});
        em.setPersistenceUnitName("sabi");

        JpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
    }

    @Bean
    public DataSource dataSource() {
        // FIXME: 13.11.2015 The credentials needs to be weaved later by an property replacer during the maven build. Where the crendentials com from an maven profile of th elocal settings.xml
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/sabi");
        dataSource.setUsername("sabiapp");
        dataSource.setPassword("sabi123");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);

        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("eclipselink.ddl-generation", "create-or-extend-tables");
        properties.setProperty("eclipselink.ddl-generation.output-mode", "sql-script");
        properties.setProperty("eclipselink.create-ddl-jdbc-file-name", "createDDL_ddlGeneration.jdbc");
        properties.setProperty("eclipselink.drop-ddl-jdbc-file-name", "dropDDL_ddlGeneration.jdbc");
        properties.setProperty("eclipselink.target-database", "MYSQL");
        properties.setProperty("eclipselink.weaving", "static");

        // To Convert CamelCase on JavaProps to Camel_Case on DB-Level,
        // as the @Column(name=) annotation will only be used when generating
        // DDL but not on runtime to do the translation trick.
        properties.setProperty("eclipselink.session.customizer", "de.bluewhale.sabi.configs.JPACamelCaseNamingStrategy");

        return properties;
    }
}
