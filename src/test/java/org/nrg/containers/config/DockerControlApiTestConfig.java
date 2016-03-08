package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.prefs.services.PreferenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.nrg.containers.api")
public class DockerControlApiTestConfig {
    @Bean
    public PreferenceService mockPreferenceService() {
        return Mockito.mock(PreferenceService.class);
    }
}
