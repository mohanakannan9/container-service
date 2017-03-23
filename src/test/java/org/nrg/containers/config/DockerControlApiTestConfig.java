package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.daos.ContainerEntityRepository;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.framework.services.NrgEventService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ObjectMapperConfig.class})
public class DockerControlApiTestConfig {
    @Bean
    public DockerControlApi dockerControlApi(final DockerServerPrefsBean containerServerPref,
                                             final ObjectMapper objectMapper,
                                             final NrgEventService eventService) {
        return new DockerControlApi(containerServerPref, objectMapper, eventService);
    }

    @Bean
    public DockerServerPrefsBean dockerServerPrefsBean(final NrgPreferenceService nrgPreferenceService) {
        return new DockerServerPrefsBean(nrgPreferenceService);
    }

    @Bean
    public NrgPreferenceService mockPrefsService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public ContainerEntityService mockContainerEntityService() {
        return Mockito.mock(ContainerEntityService.class);
    }

    @Bean
    public ContainerEntityRepository mockContainerExecutionRepository() {
        return Mockito.mock(ContainerEntityRepository.class);
    }

    @Bean
    public NrgEventService mockNrgEventService() {
        return Mockito.mock(NrgEventService.class);
    }
}
