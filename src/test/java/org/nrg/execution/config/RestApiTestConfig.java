package org.nrg.execution.config;

import org.mockito.Mockito;
import org.nrg.execution.rest.DockerRestApi;
import org.nrg.execution.services.DockerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class RestApiTestConfig {
    @Bean
    public DockerRestApi dockerRestApi() {
        return new DockerRestApi();
    }

    @Bean
    public DockerService mockDockerService() {
        return Mockito.mock(DockerService.class);
    }
}
