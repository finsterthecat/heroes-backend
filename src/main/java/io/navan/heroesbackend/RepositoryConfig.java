package io.navan.heroesbackend;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class RepositoryConfig implements RepositoryRestConfigurer {
     
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(Hero.class);
        RepositoryRestConfigurer.super.configureRepositoryRestConfiguration(config, cors);
    }

}
