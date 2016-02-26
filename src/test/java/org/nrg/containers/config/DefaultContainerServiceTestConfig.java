package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.impl.DefaultContainerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
//@ComponentScan(
//        value = "org.nrg.containers.services, " +
//                "org.nrg.containers.services.impl, " +
//                "org.nrg.containers.api, " +
//                "org.nrg.containers.metadata, " +
//                "org.nrg.containers.metadata.service",
//        resourcePattern = "*.class")
public class DefaultContainerServiceTestConfig {
    @Bean
    public ContainerControlApi mockContainerControlApi() {
        return Mockito.mock(ContainerControlApi.class);
    }

    @Bean
    public ImageMetadataService mockImageMetadataService() {
        return Mockito.mock(ImageMetadataService.class);
    }

    @Bean
    public ContainerService containerService() {
        return new DefaultContainerService();
    }
}
