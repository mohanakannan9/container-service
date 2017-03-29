package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.containers.services.DockerService;
import org.nrg.containers.services.impl.DockerServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import({IntegrationTestConfig.class})
public class DockerServiceIntegrationTestConfig {
    @Bean
    public DockerService dockerService(final ContainerControlApi controlApi,
                                       final DockerHubService dockerHubService,
                                       final CommandService commandService,
                                       final DockerServerPrefsBean dockerServerPrefsBean,
                                       final ObjectMapper objectMapper) {
        return new DockerServiceImpl(controlApi, dockerHubService, commandService, dockerServerPrefsBean, objectMapper);
    }

    @Bean
    public DockerHubService mockDockerHubService() {
        return Mockito.mock(DockerHubService.class);
    }
}
