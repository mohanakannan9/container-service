package org.nrg.actions.config;

import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.transporter.TransportService;
import org.nrg.transporter.config.TransporterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration
@XnatPlugin(value = "actions", description = "Action Command Execution Service", entityPackages = "org.nrg.actions")
@ComponentScan(value = "org.nrg.actions",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
@Import(TransporterConfig.class)
public class ActionsConfig {
}