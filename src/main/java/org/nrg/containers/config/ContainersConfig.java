package org.nrg.containers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.containers.events.DockerEventPuller;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.transporter.config.TransporterConfig;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.services.PermissionsServiceI;
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

    // This should not be here, this bean should live somewhere else.
    // But the Permissions class does some goofy half-context-half-reflection thing,
    // with the implementation class stored as a preference for some reason.
    // So it is what it is. I'll remove it later when that is updated.
    // See XNAT-4647
    @Bean
    public PermissionsServiceI permissionsService() {
        return Permissions.getPermissionsService();
    }

    @Bean
    public TriggerTask dockerEventPullerTask(final DockerEventPuller dockerEventPuller) {
        return new TriggerTask(
                dockerEventPuller,
                new PeriodicTrigger(10L, TimeUnit.SECONDS)
        );
    }
}