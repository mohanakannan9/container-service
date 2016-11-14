package org.nrg.containers.config;

import org.mockito.Mockito;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.RoleServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestApiTestConfig {
    @Bean
    public RoleHolder mockRoleHolder(final RoleServiceI roleServiceI) {
        return new RoleHolder(roleServiceI);
    }

    @Bean
    public RoleServiceI mockRoleService() {
        return Mockito.mock(RoleServiceI.class);
    }

    @Bean
    public UserManagementServiceI mockUserManagementServiceI() {
        return Mockito.mock(UserManagementServiceI.class);
    }
}
