package org.nrg.actions.config;

import org.mockito.Mockito;
import org.nrg.actions.rest.CommandRestApi;
import org.nrg.actions.services.CommandService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class CommandRestApiTestConfig {
    @Bean
    public CommandService mockCommandService() {
        return Mockito.mock(CommandService.class);
    }

    @Bean
    public CommandRestApi commandRestApi() {
        return new CommandRestApi();
    }
}
