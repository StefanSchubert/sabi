/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Author: Stefan Schubert
 * Date: 05.09.15
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories("de.bluewhale.sabi.persistence.repositories")
@EnableJpaAuditing
public class JPAConfig {

    @Value("${eclipselink.target-database:Auto}")
    private String eclipseLinkTargetDatabase;

    @Value("${eclipselink.logging.level:INFO}")
    private String eclipseLinkLoggingLevel;

    @Value("${h2.test.mode}")
    private Boolean h2TestMode;

    @Autowired
    private DataSource dataSource;


    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(new String[]{"de.bluewhale.sabi.persistence.model"});
        em.setPersistenceUnitName("sabi");

        //  JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        JpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(additionalProperties());

        return em;
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

        if (h2TestMode) {
            properties.setProperty("eclipselink.ddl-generation.output-mode", "database");
            properties.setProperty("eclipselink.ddl-generation", "drop-and-create-tables");
        } else {
            properties.setProperty("eclipselink.ddl-generation", "create-or-extend-tables");
            properties.setProperty("eclipselink.ddl-generation.output-mode", "sql-script");
        }

        properties.setProperty("eclipselink.create-ddl-jdbc-file-name", "createDDL_ddlGeneration.jdbc");
        properties.setProperty("eclipselink.drop-ddl-jdbc-file-name", "dropDDL_ddlGeneration.jdbc");
        properties.setProperty("eclipselink.target-database", eclipseLinkTargetDatabase);
        properties.setProperty("eclipselink.weaving", "static");

        // only for debugging more jpa logging
        /*
        OFF	This setting disables the generation of the log output. You may want to set logging to OFF during production to avoid the overhead of logging.
        SEVERE	This level enables reporting of failure cases only. Usually, if the failure occurs, the application stops.
        WARNING	This level enables logging of issues that have a potential to cause problems. For example, a setting that is picked by the application and not by the user.
        INFO	This level enables the standard output. The contents of this output is very limited. It is the default logging level if a logging level is not set.
        CONFIG	This level enables logging of such configuration details as your database login information and some metadata information. You may want to use the CONFIG log level at deployment time.
        FINE	This level enables logging of the first level of the debugging information and SQL. You may want to use this log level during debugging and testing, but not at production.
        FINER	This level enables logging of more debugging information than the FINE setting. For example, the transaction information is logged at this level. You may want to use this log level during debugging and testing, but not at production.
        FINEST	This level enables logging of more debugging information than the FINER setting, such as a very detailed information about certain features (for example, sequencing). You may want to use this log level during debugging and testing, but not at production.
        ALL	This level currently logs at the same level as FINEST.
         */
        properties.setProperty("eclipselink.logging.level", eclipseLinkLoggingLevel);
        properties.setProperty("eclipselink.logging.exception", "true");

        // To Convert CamelCase on JavaProps to Camel_Case on DB-Level,
        // as the @Column(name=) annotation will only be used when generating
        // DDL but not on runtime to do the translation trick.
        properties.setProperty("eclipselink.session.customizer", "de.bluewhale.sabi.configs.JPACamelCaseNamingStrategy");

        return properties;
    }
}
