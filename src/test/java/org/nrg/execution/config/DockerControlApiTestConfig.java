package org.nrg.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.nrg.execution.api.DockerControlApi;
import org.nrg.execution.daos.ContainerExecutionRepository;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.DockerServerPrefsBean;
import org.nrg.execution.services.ContainerExecutionService;
import org.nrg.execution.services.HibernateContainerExecutionService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerControlApiTestConfig {
    @Bean
    public DockerControlApi dockerControlApi() {
        return new DockerControlApi();
    }

    @Bean
    public DockerServerPrefsBean dockerServerPrefsBean() {
        return new DockerServerPrefsBean();
    }

    @Bean
    public NrgPreferenceService mockPrefsService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public ContainerExecutionService mockContainerExecutionService() {
        return Mockito.mock(HibernateContainerExecutionService.class);
    }

    @Bean
    public ContainerExecutionRepository mockContainerExecutionRepository() {
        return Mockito.mock(ContainerExecutionRepository.class);
    }

    @Bean
    public NrgEventService mockNrgEventService() {
        return Mockito.mock(NrgEventService.class);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
