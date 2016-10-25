package org.nrg.containers.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.spotify.docker.client.DockerClient;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.config.DockerIntegrationTestConfig;
import org.nrg.containers.model.*;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
import org.nrg.framework.services.ContextService;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.ResourceTransactionManager;

import javax.transaction.TransactionManager;
import java.io.File;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
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
    @Autowired private AliasTokenService mockAliasTokenService;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Autowired private ResourceTransactionManager txManager;

    private DockerClient client;
    private final String BUSYBOX_LATEST = "busybox:latest";

    Date lastEventCheckTime;

    UserI mockUser;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

    @Before
    public void setup() throws Exception {

        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        final String containerHost = StringUtils.isBlank(hostEnv) ? defaultHost : hostEnv;

        // Mock out the prefs bean
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

        // Mock the userI
        mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn("mockUser");

        // Mock the user management service
        when(mockUserManagementServiceI.getUser("mockUser")).thenReturn(mockUser);

        // Mock the aliasTokenService
        final AliasToken mockAliasToken = new AliasToken();
        mockAliasToken.setAlias("alias");
        mockAliasToken.setSecret("secret");
        when(mockAliasTokenService.issueTokenForUser(mockUser)).thenReturn(mockAliasToken);

        // Mock the site config preferences
        when(mockSiteConfigPreferences.getSiteUrl()).thenReturn("mock://url");
        when(mockSiteConfigPreferences.getBuildPath()).thenReturn(folder.newFolder().getAbsolutePath()); // transporter makes a directory under build
        when(mockSiteConfigPreferences.getArchivePath()).thenReturn(folder.newFolder().getAbsolutePath()); // container logs get stored under archive

        // Pull busybox
        client = controlApi.getClient();
        client.pull(BUSYBOX_LATEST);
    }

//    @AfterTransaction
//    public void tearDown() {
//        log.debug("I don't know what to do here.");
//    }

    @Test
    public void testSpringConfiguration() {
        assertThat(containerExecutionService, not(nullValue()));
    }

    /**
     * This test does not work. I have to mess around with the transaction so that things can be saved
     * on one thread and read on another. But Spring really does not like it when I close the transaction
     * that it started for the test. It wants to either roll it back or commit it, and either throws an
     * exception that the transaction hasn't been started. I don't know how to get spring to not do that.
     */
//    @Test
//    @Transactional
//    public void testEventPuller() throws Exception {
//        final DockerImage dockerImage = controlApi.getImageById(BUSYBOX_LATEST);
//
//        final String commandJson = "{" +
//                "\"name\":\"event-test\"," +
//                "\"docker-image\": \"" + dockerImage.getImageId() + "\"," +
//                "\"run\": {" +
//                    "\"command-line\":\"echo hello world\"," +
//                    "\"mounts\":[" +
//                        "{\"name\":\"output\", \"type\":\"output\", \"path\":\"/output\"}" +
//                    "]" +
//                "}}";
//        final Command command = mapper.readValue(commandJson, Command.class);
//        final Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
//        if (!transaction.isActive()) {
//            transaction.begin();
//        }
//        commandService.create(command);
//
////        final ResolvedCommand resolved = commandService.resolveCommand(command);
//
//        final ContainerExecution execution =
//                commandService.resolveAndLaunchCommand(command.getId(), Maps.<String, String>newHashMap(), mockUser);
//        final List<ContainerExecution> executions = containerExecutionService.getAll();
//        assertThat(executions, not(Matchers.<ContainerExecution>empty()));
//        log.info("Found at least one execution: " + executions.get(0).getId());
//
//        log.info("Committing transaction");
//        transaction.commit();
//        log.info("Starting new transaction");
//        final Transaction newTransaction = sessionFactory.getCurrentSession().beginTransaction();
//        assertTrue(newTransaction.isActive());
//
//        log.info("Sleeping");
//        Thread.sleep(5000);
//
//        log.info("Refreshing execution entity");
//        containerExecutionService.refresh(execution);
//        log.info("Checking execution history");
//        final List<ContainerExecutionHistory> history = execution.getHistory();
//        assertThat(history, not(Matchers.<ContainerExecutionHistory>empty()));
//
//        log.debug("Test is over. Everything else is Spring.");
//        sessionFactory.getCurrentSession().clear();
//    }
}
