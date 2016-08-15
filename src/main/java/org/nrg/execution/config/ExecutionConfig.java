package org.nrg.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.execution.events.DockerEventPuller;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.transporter.config.TransporterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@Configuration
@XnatPlugin(value = "execution", description = "Action/Command Execution Service", entityPackages = "org.nrg.execution")
@ComponentScan(value = "org.nrg.execution",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
@Import(TransporterConfig.class)
public class ExecutionConfig {
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