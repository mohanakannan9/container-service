package org.nrg.execution.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.transporter.config.TransporterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

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
}