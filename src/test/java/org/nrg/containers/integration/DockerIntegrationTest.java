package org.nrg.containers.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.spotify.docker.client.DockerClient;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.config.DockerIntegrationTestConfig;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionHistory;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DockerIntegrationTestConfig.class)
public class DockerIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(DockerIntegrationTest.class);

    @Autowired private ObjectMapper mapper;
    @Autowired private DockerControlApi controlApi;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;
    @Autowired private CommandService commandService;
    @Autowired private ContainerExecutionService containerExecutionService;
    @Autowired private SessionFactory sessionFactory;

    private DockerClient client;
    private final String BUSYBOX_LATEST = "busybox:latest";

    Date lastEventCheckTime;

    @Before
    @Transactional
    public void setup() throws Exception {

        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        final String containerHost = StringUtils.isBlank(hostEnv) ? defaultHost : hostEnv;

        when(mockDockerServerPrefsBean.getHost()).thenReturn(containerHost);
        when(mockDockerServerPrefsBean.toDto()).thenCallRealMethod();

        lastEventCheckTime = new Date();
        when(mockDockerServerPrefsBean.getLastEventCheckTime()).thenAnswer(new Answer<Date>() {
            @Override
            public Date answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return lastEventCheckTime;
            }
        });
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] args = invocationOnMock.getArguments();
                lastEventCheckTime = (Date)args[0];
                return null;
            }
        })
        .when(mockDockerServerPrefsBean).setLastEventCheckTime(Mockito.any(Date.class));

        client = controlApi.getClient();
        client.pull(BUSYBOX_LATEST);
    }

    @Test
    public void testSpringConfiguration() {
        assertThat(containerExecutionService, not(nullValue()));
    }

    @Test
    @Transactional
    public void testEventPuller() throws Exception {
        final DockerImage dockerImage = controlApi.getImageById(BUSYBOX_LATEST);

        final String commandJson = "{" +
                "\"name\":\"event-test\"," +
                "\"docker-image\": \"" + dockerImage.getImageId() + "\"," +
                "\"run-template\":[\"echo\", \"hello\", \"world\"]," +
                "\"mounts-out\":[" +
                    "{\"name\":\"output\", \"remote-path\":\"/output\"}" +
                "]}";
        final Command command = mapper.readValue(commandJson, Command.class);
        final Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
        commandService.create(command);

        final UserI mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn("mockUser");

//        final ResolvedCommand resolved = commandService.resolveCommand(command);

        final ContainerExecution execution =
                commandService.launchCommand(command.getId(), Maps.<String, String>newHashMap(), mockUser);
        final List<ContainerExecution> executions = containerExecutionService.getAll();
        assertThat(executions, not(Matchers.<ContainerExecution>empty()));
        log.info("Found at least one execution: " + executions.get(0).getId());

        log.info("Committing transaction");
        transaction.commit();
        log.info("Starting new transaction");
        final Transaction newTransaction = sessionFactory.getCurrentSession().beginTransaction();
        assertTrue(newTransaction.isActive());

        log.info("Sleeping");
        Thread.sleep(5000);

        log.info("Refreshing execution entity");
        containerExecutionService.refresh(execution);
        log.info("Checking execution history");
        final List<ContainerExecutionHistory> history = execution.getHistory();
        assertThat(history, not(Matchers.<ContainerExecutionHistory>empty()));
    }
}
