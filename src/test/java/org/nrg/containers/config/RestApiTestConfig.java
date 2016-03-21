package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.services.ContainerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan("org.nrg.containers.rest")
public class RestApiTestConfig {
    @Bean
    public ContainerService mockContainerService() {
        return Mockito.mock(ContainerService.class);
    }

    @Bean
    public ContainerServer mockContainerServer() {
        return Mockito.mock(ContainerServer.class);
    }
}
