package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerImageService;
import org.nrg.containers.services.DockerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
    public DockerImageService mockDockerImageService() {
        return Mockito.mock(DockerImageService.class);
    }

    @Bean
    public DockerService mockDockerService() {
        return Mockito.mock(DockerService.class);
    }
}
