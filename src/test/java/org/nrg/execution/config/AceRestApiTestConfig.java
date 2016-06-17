package org.nrg.execution.config;

import org.mockito.Mockito;
import org.nrg.execution.rest.AceRestApi;
import org.nrg.execution.services.AceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class AceRestApiTestConfig {
    @Bean
    public AceService mockAceService() {
        return Mockito.mock(AceService.class);
    }

    @Bean
    public AceRestApi aceRestApi() {
        return new AceRestApi();
    }

//    @Bean
//    public ActionService mockActionService() {
//        return Mockito.mock(ActionService.class);
//    }
//
//    @Bean
//    public CommandService mockCommandService() {
//        return Mockito.mock(CommandService.class);
//    }
//
//    @Bean
//    public TransportService mockTransportService() {
//        return Mockito.mock(TransportService.class);
//    }
//
//    @Bean
//    public ContainerControlApi mockContainerControlApi() {
//        return Mockito.mock(ContainerControlApi.class);
//    }

}
