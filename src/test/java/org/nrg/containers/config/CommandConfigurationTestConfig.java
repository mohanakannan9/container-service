package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.containers.services.impl.ContainerConfigServiceImpl;
import org.nrg.xdat.security.services.RoleHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ObjectMapperConfig.class})
public class CommandConfigurationTestConfig {
    @Bean
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public CommandService commandService() {
        return Mockito.mock(CommandService.class);
    }

    @Bean
    public RoleHolder roleHolder() {
        return Mockito.mock(RoleHolder.class);
    }

    @Bean
    public ContainerConfigService containerConfigService(final ConfigService configService,
                                                         final ObjectMapper objectMapper) {
        return new ContainerConfigServiceImpl(configService, objectMapper);
    }
}
