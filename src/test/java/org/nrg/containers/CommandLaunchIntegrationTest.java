package org.nrg.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nrg.containers.api.DockerControlApi;
import org.nrg.containers.config.IntegrationTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.Container.ContainerMount;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerService;
import org.nrg.containers.services.DockerServerService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.impl.ExptURI;
import org.nrg.xnat.helpers.uri.archive.impl.ProjURI;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PrepareForTest(UriParserUtils.class)
@PowerMockIgnore({"org.apache.*", "java.*", "javax.*"})
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Transactional
public class CommandLaunchIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(CommandLaunchIntegrationTest.class);

    private UserI mockUser;

    private final String FAKE_USER = "mockUser";
    private final String FAKE_ALIAS = "alias";
    private final String FAKE_SECRET = "secret";
    private final String FAKE_HOST = "mock://url";

    private static DockerClient CLIENT;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private ContainerService containerService;
    @Autowired private DockerControlApi controlApi;
    @Autowired private AliasTokenService mockAliasTokenService;
    @Autowired private DockerServerService mockDockerServerService;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;
    @Autowired private PermissionsServiceI mockPermissionsServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("user.dir") + "/build"));

    @Before
    public void setup() throws Exception {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });

        // Mock out the prefs bean
        final String defaultHost = "unix:///var/run/docker.sock";
        final String hostEnv = System.getenv("DOCKER_HOST");
        final String certPathEnv = System.getenv("DOCKER_CERT_PATH");
        final String tlsVerify = System.getenv("DOCKER_TLS_VERIFY");

        final boolean useTls = tlsVerify != null && tlsVerify.equals("1");
        final String certPath;
        if (useTls) {
            if (StringUtils.isBlank(certPathEnv)) {
                throw new Exception("Must set DOCKER_CERT_PATH if DOCKER_TLS_VERIFY=1.");
            }
            certPath = certPathEnv;
        } else {
            certPath = "";
        }

        final String containerHost;
        if (StringUtils.isBlank(hostEnv)) {
            containerHost = defaultHost;
        } else {
            final Pattern tcpShouldBeHttpRe = Pattern.compile("tcp://.*");
            final java.util.regex.Matcher tcpShouldBeHttpMatch = tcpShouldBeHttpRe.matcher(hostEnv);
            if (tcpShouldBeHttpMatch.matches()) {
                // Must switch out tcp:// for either http:// or https://
                containerHost = hostEnv.replace("tcp://", "http" + (useTls ? "s" : "") + "://");
            } else {
                containerHost = hostEnv;
            }
        }

        final DockerServer dockerServer = DockerServer.create(0L, "name", containerHost, certPath, false);
        when(mockDockerServerService.getServer()).thenReturn(dockerServer);

        // Mock the userI
        mockUser = mock(UserI.class);
        when(mockUser.getLogin()).thenReturn(FAKE_USER);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USER)).thenReturn(mockUser);

        // Mock UriParserUtils using PowerMock. This allows us to mock out
        // the responses to its static method parseURI().
        mockStatic(UriParserUtils.class);

        // Mock the aliasTokenService
        final AliasToken mockAliasToken = new AliasToken();
        mockAliasToken.setAlias(FAKE_ALIAS);
        mockAliasToken.setSecret(FAKE_SECRET);
        when(mockAliasTokenService.issueTokenForUser(mockUser)).thenReturn(mockAliasToken);

        // Mock the site config preferences
        when(mockSiteConfigPreferences.getSiteUrl()).thenReturn(FAKE_HOST);
        when(mockSiteConfigPreferences.getBuildPath()).thenReturn(folder.newFolder().getAbsolutePath()); // transporter makes a directory under build
        when(mockSiteConfigPreferences.getArchivePath()).thenReturn(folder.newFolder().getAbsolutePath()); // container logs get stored under archive
        when(mockSiteConfigPreferences.getProperty("processingUrl", FAKE_HOST)).thenReturn(FAKE_HOST);

        CLIENT = controlApi.getClient();
    }

    private boolean canConnectToDocker() {
        try {
            return CLIENT.ping().equals("OK");
        } catch (InterruptedException | DockerException e) {
            log.warn("Could not connect to docker.", e);
        }
        return false;
    }

    @Test
    public void testFakeReconAll() throws Exception {
        assumeThat(SystemUtils.IS_OS_WINDOWS_7, is(false));
        assumeThat(canConnectToDocker(), is(true));

        CLIENT.pull("busybox:latest");

        final String dir = Paths.get(ClassLoader.getSystemResource("commandLaunchTest").toURI()).toString().replace("%20", " ");
        final String commandJsonFile = Paths.get(dir, "/fakeReconAllCommand.json").toString();
        final String sessionJsonFile = Paths.get(dir, "/session.json").toString();
        final String fakeResourceDir = Paths.get(dir, "/fakeResource").toString();
        final String commandWrapperName = "recon-all-session";

        final Command fakeReconAll = mapper.readValue(new File(commandJsonFile), Command.class);
        final Command fakeReconAllCreated = commandService.create(fakeReconAll);

        CommandWrapper commandWrapper = null;
        for (final CommandWrapper commandWrapperLoop : fakeReconAllCreated.xnatCommandWrappers()) {
            if (commandWrapperName.equals(commandWrapperLoop.name())) {
                commandWrapper = commandWrapperLoop;
                break;
            }
        }
        assertThat(commandWrapper, is(not(nullValue())));

        final Session session = mapper.readValue(new File(sessionJsonFile), Session.class);
        final Scan scan = session.getScans().get(0);
        final Resource resource = scan.getResources().get(0);
        resource.setDirectory(fakeResourceDir);
        final String sessionJson = mapper.writeValueAsString(session);
        final ArchivableItem mockProjectItem = mock(ArchivableItem.class);
        final ProjURI mockUriObject = mock(ProjURI.class);
        when(UriParserUtils.parseURI("/archive" + session.getUri())).thenReturn(mockUriObject);
        when(mockUriObject.getSecurityItem()).thenReturn(mockProjectItem);
        when(mockPermissionsServiceI.canEdit(mockUser, mockProjectItem)).thenReturn(Boolean.TRUE);

        final String t1Scantype = "T1_TEST_SCANTYPE";

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionJson);
        runtimeValues.put("T1-scantype", t1Scantype);

        final Container execution = containerService.resolveCommandAndLaunchContainer(commandWrapper.id(), runtimeValues, mockUser);
        Thread.sleep(1000); // Wait for container to finish

        // Raw inputs
        assertThat(execution.getRawInputs(), is(runtimeValues));

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("session", session.getUri());
        expectedXnatInputValues.put("T1-scantype", t1Scantype);
        expectedXnatInputValues.put("label", session.getLabel());
        expectedXnatInputValues.put("T1", session.getScans().get(0).getUri());
        expectedXnatInputValues.put("resource", session.getScans().get(0).getResources().get(0).getUri());
        assertThat(execution.getWrapperInputs(), is(expectedXnatInputValues));

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("subject-id", session.getLabel());
        expectedCommandInputValues.put("other-recon-all-args", "-all");
        assertThat(execution.getCommandInputs(), is(expectedCommandInputValues));

        // Outputs
        // assertTrue(resolvedCommand.getOutputs().isEmpty());

        final List<String> outputNames = Lists.transform(execution.outputs(), new Function<Container.ContainerOutput, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final Container.ContainerOutput output) {
                return output == null ? "" : output.name();
            }
        });
        assertThat(outputNames, contains("data", "text-file"));

        // Environment variables
        final Map<String, String> expectedEnvironmentVariables = Maps.newHashMap();
        expectedEnvironmentVariables.put("XNAT_USER", FAKE_ALIAS);
        expectedEnvironmentVariables.put("XNAT_PASS", FAKE_SECRET);
        expectedEnvironmentVariables.put("XNAT_HOST", FAKE_HOST);
        assertThat(execution.environmentVariables(), is(expectedEnvironmentVariables));


        final List<ContainerMount> mounts = execution.mounts();
        assertThat(mounts, hasSize(2));

        ContainerMount inputMount = null;
        ContainerMount outputMount = null;
        for (final ContainerMount mount : mounts) {
            if (mount.name().equals("input")) {
                inputMount = mount;
            } else if (mount.name().equals("output")) {
                outputMount = mount;
            } else {
                fail("We should not have a mount with name " + mount.name());
            }
        }

        assertThat(inputMount, is(not(nullValue())));
        assertThat(inputMount.containerPath(), is("/input"));
        assertThat(inputMount.xnatHostPath(), is(fakeResourceDir));

        assertThat(outputMount, is(not(nullValue())));
        assertThat(outputMount.containerPath(), is("/output"));
        final String outputPath = outputMount.xnatHostPath();

        try {
            final String[] outputFileContents = readFile(outputPath + "/out.txt");
            assertThat(outputFileContents.length, greaterThanOrEqualTo(2));
            assertThat(outputFileContents[0], is("recon-all -s session1 -all"));

            final File fakeResourceDirFile = new File(fakeResourceDir);
            assertThat(fakeResourceDirFile, is(not(nullValue())));
            assertThat(fakeResourceDirFile.listFiles(), is(not(nullValue())));
            final List<String> fakeResourceDirFileNames = Lists.newArrayList();
            for (final File file : fakeResourceDirFile.listFiles()) {
                fakeResourceDirFileNames.add(file.getName());

            }
            assertThat(Lists.newArrayList(outputFileContents[1].split(" ")), is(fakeResourceDirFileNames));
        } catch (IOException e) {
            log.warn("Failed to read output files. This is not a problem if you are using docker-machine and cannot mount host directories.", e);
        }
    }

    @Test
    public void testProjectMount() throws Exception {
        assumeThat(canConnectToDocker(), is(true));

        final String dir = Paths.get(ClassLoader.getSystemResource("commandLaunchTest").toURI()).toString().replace("%20", " ");
        final String commandJsonFile = dir + "/project-mount-command.json";
        final String projectJsonFile = dir + "/project.json";
        final String projectDir = dir + "/project";
        // final String commandWrapperName = "find-in-project";

        final Command command = mapper.readValue(new File(commandJsonFile), Command.class);
        final Command commandCreated = commandService.create(command);
        final CommandWrapper commandWrapper = commandCreated.xnatCommandWrappers().get(0);
        assertThat(commandWrapper, is(not(nullValue())));

        final Project project = mapper.readValue(new File(projectJsonFile), Project.class);
        project.setDirectory(projectDir);
        final String projectJson = mapper.writeValueAsString(project);

        // Create the mock objects we will need in order to verify permissions
        final ArchivableItem mockProjectItem = mock(ArchivableItem.class);
        final ExptURI mockUriObject = mock(ExptURI.class);
        when(UriParserUtils.parseURI("/archive" + project.getUri())).thenReturn(mockUriObject);
        when(mockUriObject.getSecurityItem()).thenReturn(mockProjectItem);
        when(mockPermissionsServiceI.canEdit(mockUser, mockProjectItem)).thenReturn(Boolean.TRUE);

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectJson);

        final Container execution = containerService.resolveCommandAndLaunchContainer(commandWrapper.id(), runtimeValues, mockUser);
        Thread.sleep(1000); // Wait for container to finish

        // Raw inputs
        assertThat(execution.getRawInputs(), is(runtimeValues));

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("project", project.getUri());
        assertThat(execution.getWrapperInputs(), is(expectedXnatInputValues));

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        assertThat(execution.getCommandInputs(), is(expectedCommandInputValues));

        // Outputs by name. We will check the files later.
        final List<String> outputNames = Lists.transform(execution.outputs(), new Function<Container.ContainerOutput, String>() {
            @Override
            public String apply(final Container.ContainerOutput output) {
                return output.name();
            }
        });
        assertThat(outputNames, contains("outputs"));

        // Environment variables
        final Map<String, String> expectedEnvironmentVariables = Maps.newHashMap();
        expectedEnvironmentVariables.put("XNAT_USER", FAKE_ALIAS);
        expectedEnvironmentVariables.put("XNAT_PASS", FAKE_SECRET);
        expectedEnvironmentVariables.put("XNAT_HOST", FAKE_HOST);
        assertThat(execution.environmentVariables(), is(expectedEnvironmentVariables));

        // mounts
        final List<ContainerMount> mounts = execution.mounts();
        assertThat(mounts, hasSize(2));

        ContainerMount inputMount = null;
        ContainerMount outputMount = null;
        for (final ContainerMount mount : mounts) {
            if (mount.name().equals("input")) {
                inputMount = mount;
            } else if (mount.name().equals("output")) {
                outputMount = mount;
            } else {
                fail("We should not have a mount with name " + mount.name());
            }
        }

        assertThat(inputMount, is(not(nullValue())));
        assertThat(inputMount.containerPath(), is("/input"));
        assertThat(inputMount.xnatHostPath(), is(projectDir));

        assertThat(outputMount, is(not(nullValue())));
        assertThat(outputMount.containerPath(), is("/output"));
        final String outputPath = outputMount.xnatHostPath();

        try {
            // Read two output files: files.txt and dirs.txt
            final String[] expectedFilesFileContents = {
                    "/input/project-file.txt",
                    "/input/resource/project-resource-file.txt",
                    "/input/session/resource/session-resource-file.txt",
                    "/input/session/scan/resource/scan-resource-file.txt",
                    "/input/session/scan/scan-file.txt",
                    "/input/session/session-file.txt"
            };
            final List<String> filesFileContents = Lists.newArrayList(readFile(outputPath + "/files.txt"));
            assertThat(filesFileContents, containsInAnyOrder(expectedFilesFileContents));

            final String[] expectedDirsFileContents = {
                    "/input",
                    "/input/resource",
                    "/input/session",
                    "/input/session/resource",
                    "/input/session/scan",
                    "/input/session/scan/resource"
            };
            final List<String> dirsFileContents = Lists.newArrayList(readFile(outputPath + "/dirs.txt"));
            assertThat(dirsFileContents, containsInAnyOrder(expectedDirsFileContents));
        } catch (IOException e) {
            log.warn("Failed to read output files. This is not a problem if you are using docker-machine and cannot mount host directories.", e);
        }
    }

    @SuppressWarnings("deprecation")
    private String[] readFile(final String outputFilePath) throws IOException {
        final File outputFile = new File(outputFilePath);
        if (!outputFile.canRead()) {
            throw new IOException("Cannot read output file " + outputFile.getAbsolutePath());
        }
        return FileUtils.readFileToString(outputFile).split("\\n");
    }
}
