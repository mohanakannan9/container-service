package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.model.ContainerServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.nrg.containers.api")
public class DockerControlApiTestConfig {
    @Bean
    public ContainerServer mockPreferenceService() {
        return Mockito.mock(ContainerServer.class);
    }
}
