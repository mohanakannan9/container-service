package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.containers.services.CommandLabelService;
import org.nrg.containers.services.impl.CommandLabelServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ObjectMapperConfig.class})
public class CommandLabelServiceTestConfig {
    @Bean
    public CommandLabelService commandLabelService(final ObjectMapper objectMapper) {
        return new CommandLabelServiceImpl(objectMapper);
    }
}
