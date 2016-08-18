package org.nrg.execution.integration;

import com.spotify.docker.client.DockerClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.execution.api.DockerControlApi;
import org.nrg.execution.config.DockerIntegrationTestConfig;
import org.nrg.execution.model.DockerServerPrefsBean;
import org.nrg.execution.services.CommandService;
import org.nrg.execution.services.ContainerExecutionService;
import org.nrg.framework.scope.EntityId;
import org.nrg.prefs.services.NrgPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerIntegrationTestConfig.class)
public class DockerIntegrationTest {
    @Autowired private DockerControlApi controlApi;
    @Autowired private NrgPreferenceService mockPrefsService;
    @Autowired private DockerServerPrefsBean dockerServerPrefsBean;
    @Autowired private CommandService commandService;
    @Autowired private ContainerExecutionService containerExecutionService;

    private DockerClient client;
    private final String BUSYBOX_LATEST = "busybox:latest";

    @Before
    @Transactional
    public void setup() throws Exception {

        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        final String containerHost = StringUtils.isBlank(hostEnv) ? defaultHost : hostEnv;

        // Set up mock prefs service for all the calls that will initialize
        // the ContainerServerPrefsBean
        when(mockPrefsService.getPreferenceValue("docker-server", "host"))
                .thenReturn(containerHost);
//        when(mockPrefsService.getPreferenceValue("docker-server", "certPath"))
//                .thenReturn(certPath);
//        when(mockPrefsService.getPreferenceValue("docker-server", "lastEventCheckTime", EntityId.Default.getScope(), EntityId.Default.getEntityId()))
//                .thenReturn(timeZeroString);
        doNothing().when(mockPrefsService)
                .setPreferenceValue("docker-server", "host", "");
        doNothing().when(mockPrefsService)
                .setPreferenceValue("docker-server", "certPath", "");
        doNothing().when(mockPrefsService)
                .setPreferenceValue("docker-server", "lastEventCheckTime", "");
        when(mockPrefsService.hasPreference("docker-server", "host"))
                .thenReturn(true);
        when(mockPrefsService.hasPreference("docker-server", "certPath"))
                .thenReturn(true);
        when(mockPrefsService.hasPreference("docker-server", "lastEventCheckTime"))
                .thenReturn(true);

        client = controlApi.getClient();
        client.pull(BUSYBOX_LATEST);
    }

    @Test
    public void testSpringConfiguration() {
        assertThat(containerExecutionService, not(nullValue()));
    }
}
