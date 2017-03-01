package org.nrg.containers.config;

import org.nrg.containers.daos.CommandEntityRepository;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.impl.CommandServiceImpl;
import org.nrg.containers.services.impl.HibernateCommandEntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandConfig {
    @Bean
    public CommandService commandService(final CommandEntityService commandEntityService) {
        return new CommandServiceImpl(commandEntityService);
    }

    @Bean
    public CommandEntityService commandEntityService() {
        return new HibernateCommandEntityService();
    }

    @Bean
    public CommandEntityRepository commandEntityRepository() {
        return new CommandEntityRepository();
    }
}
