package org.nrg.containers.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.nrg.containers.rest, org.nrg.containers.services, org.nrg.containers.api")
public class ContainerServiceConfig {}
