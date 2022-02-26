package com.datarest.demo.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@RequiredArgsConstructor
public class CustomRepositoryRestConfiguration {
    private final BeanFactory beanFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void removeOld() {
        boolean b = beanFactory.containsBean(RepositoryRestController.class.getName());
    }

//    @Bean
//    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
//
//        return new CustomRequestMappingHandler();
//    }
}
