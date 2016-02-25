package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.api.impl.DockerControlApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(value = "org.nrg.containers.services, org.nrg.containers.services.impl, org.nrg.containers.api", resourcePattern = "*.class")
public class MockContainerControlConfig {
    @Bean
    public ContainerControlApi mockContainerControlApi() {
        return Mockito.mock(DockerControlApi.class);
    }
}
