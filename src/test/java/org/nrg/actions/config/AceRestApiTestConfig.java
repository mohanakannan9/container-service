package org.nrg.actions.config;

import org.mockito.Mockito;
import org.nrg.actions.rest.AceRestApi;
import org.nrg.actions.services.AceService;
import org.nrg.actions.services.ActionService;
import org.nrg.actions.services.CommandService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.transporter.TransportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
