package org.nrg.containers.config;

import org.nrg.framework.annotations.XnatPlugin;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@XnatPlugin(value = "container-service", description = "XNAT Container Service", config = ContainersSpringConfig.class)
@ComponentScan(value = "org.nrg.containers",
        excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*TestConfig.*", value = {}))
public class ContainersSpringConfig {}
