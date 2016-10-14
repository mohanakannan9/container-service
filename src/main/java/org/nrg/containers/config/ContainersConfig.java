package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.containers.events.DockerEventPuller;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.transporter.config.TransporterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

@Configuration
@XnatPlugin(value = "containers", name = "containers", description = "Container Service", entityPackages = "org.nrg.containers")
@ComponentScan(value = "org.nrg.containers",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
@Import(TransporterConfig.class)
public class ContainersConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TriggerTask dockerEventPullerTask(final DockerEventPuller dockerEventPuller) {
        return new TriggerTask(
                dockerEventPuller,
                new PeriodicTrigger(10L, TimeUnit.SECONDS)
        );
    }
}