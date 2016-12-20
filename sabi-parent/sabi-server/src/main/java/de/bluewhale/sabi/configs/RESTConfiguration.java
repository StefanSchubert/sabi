package de.bluewhale.sabi.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/**
 *
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
@Configuration
public class RESTConfiguration
{
    @Bean
    public View jsonTemplate() {
        MappingJackson2JsonView view = new MappingJackson2JsonView();
        view.setPrettyPrint(true);
        return view;
    }

    @Bean
    public org.springframework.web.servlet.ViewResolver viewResolver() {
        return new BeanNameViewResolver();
    }

    /*

    // NOT required! Through binding the dispatcher servlet of the WebAppInitializer
    // to the "/api/*" for the rest controller, the swagger-ui can be handled.

    @Bean
    public ViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setPrefix("/sabi/swagger");
        bean.setSuffix("*.html");
        return bean;
    }*/

}