package org.nrg.execution.config;

import org.mockito.Mockito;
import org.nrg.execution.model.DockerServerPrefsBean;
import org.nrg.execution.api.DockerControlApi;
import org.nrg.execution.services.AceService;
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
}
