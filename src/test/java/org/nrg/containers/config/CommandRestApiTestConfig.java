package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.rest.CommandRestApi;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@EnableWebSecurity
@Import({CommandTestConfig.class, RestApiTestConfig.class})
public class CommandRestApiTestConfig extends WebSecurityConfigurerAdapter {
    @Bean
    public CommandRestApi commandRestApi(final CommandService commandService,
                                         final ContainerService containerService,
                                         final UserManagementServiceI userManagementServiceI,
                                         final RoleHolder roleHolder) {
        return new CommandRestApi(commandService, containerService, userManagementServiceI, roleHolder);
    }

    @Bean
    public DockerServerPrefsBean dockerServerPrefsBean() {
        return Mockito.mock(DockerServerPrefsBean.class);
    }

    @Bean
    public ContextService contextService(final ApplicationContext applicationContext) {
        final ContextService contextService = new ContextService();
        contextService.setApplicationContext(applicationContext);
        return contextService;
    }

    @Bean
    public PermissionsServiceI permissionsService() {
        return Mockito.mock(PermissionsServiceI.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new TestingAuthenticationProvider());
    }

}
