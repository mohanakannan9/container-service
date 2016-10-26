package org.nrg.containers.config;

import org.nrg.containers.rest.CommandRestApi;
import org.nrg.containers.services.CommandService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@Import(CommandTestConfig.class)
public class CommandRestApiTestConfig {
    @Bean
    public CommandRestApi commandRestApi(final CommandService commandService) {
        return new CommandRestApi(commandService);
    }
}
