package org.nrg.containers.config;

import org.nrg.framework.annotations.XnatModule;
import org.nrg.xnat.configuration.PreferencesConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration
@XnatModule(value = "container-service", description = "XNAT Container Service", config = ContainersSpringConfig.class)
@ComponentScan(value = "org.nrg.containers",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
@Import(PreferencesConfig.class)
public class ContainersSpringConfig {}
