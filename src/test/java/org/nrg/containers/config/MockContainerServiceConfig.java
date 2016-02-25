package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.impl.DefaultContainerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(value = "org.nrg.containers.services, org.nrg.containers.rest", resourcePattern = "*.class")
public class MockContainerServiceConfig {
    @Bean
    public ContainerService mockContainerService() {
        return Mockito.mock(DefaultContainerService.class);
    }
}
