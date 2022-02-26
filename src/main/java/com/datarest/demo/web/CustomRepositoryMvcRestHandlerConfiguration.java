package com.datarest.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.geo.GeoModule;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.webmvc.BasePathAwareHandlerMapping;
import org.springframework.data.rest.webmvc.config.CorsConfigurationAware;
import org.springframework.data.rest.webmvc.config.DelegatingHandlerMapping;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.support.JpaHelper;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalConfiguration;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@Import({SpringDataJacksonConfiguration.class, //
        EnableSpringDataWebSupport.QuerydslActivator.class})
public class CustomRepositoryMvcRestHandlerConfiguration extends RepositoryRestMvcConfiguration {
    private final ApplicationContext applicationContext;
    private final ObjectProvider<PathPatternParser> parser;

    public CustomRepositoryMvcRestHandlerConfiguration(ApplicationContext context,
                                                       ObjectFactory<ConversionService> conversionService,
                                                       ObjectProvider<LinkRelationProvider> relProvider,
                                                       ObjectProvider<CurieProvider> curieProvider,
                                                       ObjectProvider<HalConfiguration> halConfiguration,
                                                       ObjectProvider<ObjectMapper> objectMapper,
                                                       ObjectProvider<RepresentationModelProcessorInvoker> invoker,
                                                       ObjectProvider<MessageResolver> resolver,
                                                       ObjectProvider<GeoModule> geoModule,
                                                       ObjectProvider<PathPatternParser> parser,
                                                       ApplicationContext applicationContext,
                                                       ObjectProvider<PathPatternParser> parser1) {
        super(context, conversionService, relProvider, curieProvider, halConfiguration, objectMapper, invoker, resolver, geoModule, parser);
        this.applicationContext = applicationContext;
        this.parser = parser1;
    }

    @Bean
    public DelegatingHandlerMapping restHandlerMapping(Repositories repositories,
                                                       RepositoryResourceMappings resourceMappings,
                                                       Optional<JpaHelper> jpaHelper,
                                                       RepositoryRestConfiguration repositoryRestConfiguration,
                                                       CorsConfigurationAware corsRestConfiguration) {

        Map<String, CorsConfiguration> corsConfigurations = corsRestConfiguration.getCorsConfigurations();
        PathPatternParser parser = this.parser.getIfAvailable();

        CustomRepositoryRestHandlerMapping repositoryMapping = new CustomRepositoryRestHandlerMapping(resourceMappings,
                repositoryRestConfiguration,
                repositories);
        repositoryMapping.setJpaHelper(jpaHelper.orElse(null));
        repositoryMapping.setApplicationContext(applicationContext);
        repositoryMapping.setCorsConfigurations(corsConfigurations);
        repositoryMapping.setPatternParser(parser);
        repositoryMapping.afterPropertiesSet();

        BasePathAwareHandlerMapping basePathMapping = new BasePathAwareHandlerMapping(repositoryRestConfiguration);
        basePathMapping.setApplicationContext(applicationContext);
        basePathMapping.setCorsConfigurations(corsConfigurations);
        basePathMapping.setPatternParser(parser);
        basePathMapping.afterPropertiesSet();

        List<HandlerMapping> mappings = new ArrayList<>();
        mappings.add(basePathMapping);
        mappings.add(repositoryMapping);

        return new DelegatingHandlerMapping(mappings, parser);
    }

}
