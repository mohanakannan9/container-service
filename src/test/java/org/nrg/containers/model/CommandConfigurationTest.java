package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.config.entities.Configuration;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.config.CommandConfigurationTestConfig;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.framework.constants.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.nrg.containers.services.ContainerConfigService.TOOL_ID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CommandConfigurationTestConfig.class)
public class CommandConfigurationTest {

    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerConfigService containerConfigService;
    @Autowired private ConfigService mockConfigService;

    @Test
    public void testSpringConfiguration() {
        assertThat(containerConfigService, not(nullValue()));
    }

    @Test
    public void testConfigureCommandForSite() throws Exception {
        final long commandId = 12L;
        final String wrapperName = "what";
        final String username = "flip";
        final String reason = "knuckles";
        final Scope scope = Scope.Site;
        final String project = null;

        final String path = String.format("command-%d-wrapper-%s", commandId, wrapperName);

        final CommandConfiguration.CommandInputConfiguration foo = CommandConfiguration.CommandInputConfiguration.create("a", null, true);
        final CommandConfiguration.CommandOutputConfiguration bar = CommandConfiguration.CommandOutputConfiguration.create("label");
        final CommandConfiguration site = CommandConfiguration.create(ImmutableMap.of("foo", foo), ImmutableMap.of("bar", bar));
        final String contents = mapper.writeValueAsString(site);

        final Configuration mockConfiguration = Mockito.mock(Configuration.class);
        when(mockConfiguration.getContents()).thenReturn(contents);
        when(mockConfigService.getConfig(TOOL_ID, path, scope, project)).thenReturn(mockConfiguration);

        containerConfigService.configureForSite(commandId, wrapperName, site, username, reason);
        final CommandConfiguration retrieved = containerConfigService.getSiteConfiguration(commandId, wrapperName);
        assertEquals(site, retrieved);
    }
}
