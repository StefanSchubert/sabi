package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.services.UserService;
import de.bluewhale.sabi.services.UserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Created with IntelliJ IDEA.
 * User: Stefan Schubert
 * Date: 04.09.15
 */
@Configuration
@ComponentScan(basePackages = "de.bluewhale.sabi")
@EnableWebMvc
// @ImportResource( { "classpath*:/rest_config.xml" } )
// @PropertySource({ "classpath:rest.properties", "classpath:web.properties" })
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public UserService userService() {
        return new UserServiceImpl();
    }
}
