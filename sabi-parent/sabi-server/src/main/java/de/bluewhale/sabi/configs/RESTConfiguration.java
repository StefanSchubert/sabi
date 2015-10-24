package de.bluewhale.sabi.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

/**
 * Created with IntelliJ IDEA.
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
}