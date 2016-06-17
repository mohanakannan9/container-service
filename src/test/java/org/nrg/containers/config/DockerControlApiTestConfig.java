package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.api.DockerControlApi;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
}
