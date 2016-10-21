package org.nrg.containers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({IntegrationTestConfig.class})
public class CommandResolutionTestConfig {
}
