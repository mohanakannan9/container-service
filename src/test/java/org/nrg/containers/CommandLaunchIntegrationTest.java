package org.nrg.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nrg.containers.config.IntegrationTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.containers.model.server.docker.DockerServerPrefsBean;
import org.nrg.containers.model.xnat.Project;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Transactional
public class CommandLaunchIntegrationTest {
    private UserI mockUser;

    private final String FAKE_USER = "mockUser";
    private final String FAKE_ALIAS = "alias";
    private final String FAKE_SECRET = "secret";
    private final String FAKE_HOST = "mock://url";

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandService commandService;
    @Autowired private ContainerService containerService;
    @Autowired private ContainerEntityService containerEntityService;
    @Autowired private AliasTokenService mockAliasTokenService;
    @Autowired private DockerServerPrefsBean mockDockerServerPrefsBean;
    @Autowired private SiteConfigPreferences mockSiteConfigPreferences;
    @Autowired private UserManagementServiceI mockUserManagementServiceI;

    @Rule public TemporaryFolder folder = new TemporaryFolder(new File("/tmp"));

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
        final String containerHost = "unix:///var/run/docker.sock";
        when(mockDockerServerPrefsBean.getHost()).thenReturn(containerHost);
        when(mockDockerServerPrefsBean.toPojo()).thenCallRealMethod();

        // Mock the userI
        mockUser = Mockito.mock(UserI.class);
        when(mockUser.getLogin()).thenReturn(FAKE_USER);

        // Mock the user management service
        when(mockUserManagementServiceI.getUser(FAKE_USER)).thenReturn(mockUser);

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
    }

    @Test
    public void testFakeReconAll() throws Exception {
        final String dir = Resources.getResource("commandLaunchTest").getPath().replace("%20", " ");
        final String commandJsonFile = dir + "/fakeReconAllCommand.json";
        final String sessionJsonFile = dir + "/session.json";
        final String fakeResourceDir = dir + "/fakeResource";
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

        final String t1Scantype = "T1_TEST_SCANTYPE";

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionJson);
        runtimeValues.put("T1-scantype", t1Scantype);

        final ContainerEntity execution = containerService.resolveCommandAndLaunchContainer(commandWrapper.id(), runtimeValues, mockUser);
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

        final List<String> outputNames = Lists.transform(execution.getOutputs(), new Function<ContainerEntityOutput, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final ContainerEntityOutput output) {
                return output == null ? "" : output.getName();
            }
        });
        assertThat(outputNames, contains("data", "text-file"));

        // Environment variables
        final Map<String, String> expectedEnvironmentVariables = Maps.newHashMap();
        expectedEnvironmentVariables.put("XNAT_USER", FAKE_ALIAS);
        expectedEnvironmentVariables.put("XNAT_PASS", FAKE_SECRET);
        expectedEnvironmentVariables.put("XNAT_HOST", FAKE_HOST);
        assertThat(execution.getEnvironmentVariables(), is(expectedEnvironmentVariables));


        final List<ContainerEntityMount> mounts = execution.getMounts();
        assertThat(mounts, hasSize(2));

        ContainerEntityMount inputMount = null;
        ContainerEntityMount outputMount = null;
        for (final ContainerEntityMount mount : mounts) {
            if (mount.getName().equals("input")) {
                inputMount = mount;
            } else if (mount.getName().equals("output")) {
                outputMount = mount;
            } else {
                fail("We should not have a mount with name " + mount.getName());
            }
        }

        assertThat(inputMount, is(not(nullValue())));
        assertThat(inputMount.getContainerPath(), is("/input"));
        assertThat(inputMount.getXnatHostPath(), is(fakeResourceDir));

        assertThat(outputMount, is(not(nullValue())));
        assertThat(outputMount.getContainerPath(), is("/output"));
        final String outputPath = outputMount.getXnatHostPath();

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
    }

    @Test
    public void testProjectMount() throws Exception {
        final String dir = Resources.getResource("commandLaunchTest").getPath().replace("%20", " ");
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

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("project", projectJson);

        final ContainerEntity execution = containerService.resolveCommandAndLaunchContainer(commandWrapper.id(), runtimeValues, mockUser);
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
        final List<String> outputNames = Lists.transform(execution.getOutputs(), new Function<ContainerEntityOutput, String>() {
            @Override
            public String apply(final ContainerEntityOutput output) {
                return output.getName();
            }
        });
        assertThat(outputNames, contains("outputs"));

        // Environment variables
        final Map<String, String> expectedEnvironmentVariables = Maps.newHashMap();
        expectedEnvironmentVariables.put("XNAT_USER", FAKE_ALIAS);
        expectedEnvironmentVariables.put("XNAT_PASS", FAKE_SECRET);
        expectedEnvironmentVariables.put("XNAT_HOST", FAKE_HOST);
        assertThat(execution.getEnvironmentVariables(), is(expectedEnvironmentVariables));

        // mounts
        final List<ContainerEntityMount> mounts = execution.getMounts();
        assertThat(mounts, hasSize(2));

        ContainerEntityMount inputMount = null;
        ContainerEntityMount outputMount = null;
        for (final ContainerEntityMount mount : mounts) {
            if (mount.getName().equals("input")) {
                inputMount = mount;
            } else if (mount.getName().equals("output")) {
                outputMount = mount;
            } else {
                fail("We should not have a mount with name " + mount.getName());
            }
        }

        assertThat(inputMount, is(not(nullValue())));
        assertThat(inputMount.getContainerPath(), is("/input"));
        assertThat(inputMount.getXnatHostPath(), is(projectDir));

        assertThat(outputMount, is(not(nullValue())));
        assertThat(outputMount.getContainerPath(), is("/output"));
        final String outputPath = outputMount.getXnatHostPath();

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
    }

    private String[] readFile(final String outputFilePath) throws IOException {
        final File outputFile = new File(outputFilePath);
        if (!outputFile.canRead()) {
            fail("Cannot read output file " + outputFile.getAbsolutePath());
        }
        return FileUtils.readFileToString(outputFile).split("\\n");
    }
}
