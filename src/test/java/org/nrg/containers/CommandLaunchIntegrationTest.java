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
import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.model.ContainerEntityMount;
import org.nrg.containers.model.ContainerEntityOutput;
import org.nrg.containers.model.DockerServerPrefsBean;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.xnat.Resource;
import org.nrg.containers.model.xnat.Scan;
import org.nrg.containers.model.xnat.Session;
import org.nrg.containers.services.CommandService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        when(mockDockerServerPrefsBean.toDto()).thenCallRealMethod();

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
        assertNotNull(commandWrapper);

        final Session session = mapper.readValue(new File(sessionJsonFile), Session.class);
        final Scan scan = session.getScans().get(0);
        final Resource resource = scan.getResources().get(0);
        resource.setDirectory(fakeResourceDir);
        final String sessionJson = mapper.writeValueAsString(session);

        final String t1Scantype = "T1_TEST_SCANTYPE";

        final Map<String, String> runtimeValues = Maps.newHashMap();
        runtimeValues.put("session", sessionJson);
        runtimeValues.put("T1-scantype", t1Scantype);

        final ContainerEntity execution = containerService.resolveAndLaunchCommand(commandWrapper.id(), fakeReconAllCreated.id(), runtimeValues, mockUser);
        Thread.sleep(1000); // Wait for container to finish

        // Raw inputs
        assertEquals(runtimeValues, execution.getRawInputs());

        // xnat wrapper inputs
        final Map<String, String> expectedXnatInputValues = Maps.newHashMap();
        expectedXnatInputValues.put("session", session.getUri());
        expectedXnatInputValues.put("T1-scantype", t1Scantype);
        expectedXnatInputValues.put("label", session.getLabel());
        expectedXnatInputValues.put("T1", session.getScans().get(0).getUri());
        expectedXnatInputValues.put("resource", session.getScans().get(0).getResources().get(0).getUri());
        assertEquals(expectedXnatInputValues, execution.getWrapperInputs());

        // command inputs
        final Map<String, String> expectedCommandInputValues = Maps.newHashMap();
        expectedCommandInputValues.put("subject-id", session.getLabel());
        expectedCommandInputValues.put("other-recon-all-args", "-all");
        assertEquals(expectedCommandInputValues, execution.getCommandInputs());

        // Outputs
        // assertTrue(resolvedCommand.getOutputs().isEmpty());

        final List<String> outputNames = Lists.transform(execution.getOutputs(), new Function<ContainerEntityOutput, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final ContainerEntityOutput output) {
                return output == null ? "" : output.getName();
            }
        });
        assertEquals(Lists.newArrayList("data", "text-file"), outputNames);

        // Environment variables
        final Map<String, String> expectedEnvironmentVariables = Maps.newHashMap();
        expectedEnvironmentVariables.put("XNAT_USER", FAKE_ALIAS);
        expectedEnvironmentVariables.put("XNAT_PASS", FAKE_SECRET);
        expectedEnvironmentVariables.put("XNAT_HOST", FAKE_HOST);
        assertEquals(expectedEnvironmentVariables, execution.getEnvironmentVariables());


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

        assertNotNull(inputMount);
        assertEquals("/input", inputMount.getContainerPath());
        assertEquals(fakeResourceDir, inputMount.getXnatHostPath());

        assertNotNull(outputMount);
        assertEquals("/output", outputMount.getContainerPath());
        final String outputPath = outputMount.getXnatHostPath();
        final File outputFile = new File(outputPath + "/out.txt");
        if (!outputFile.canRead()) {
            fail("Cannot read output file " + outputFile.getAbsolutePath());
        }
        final String[] outputFileContents = FileUtils.readFileToString(outputFile).split("\\n");
        assertThat(outputFileContents.length, greaterThanOrEqualTo(2));
        assertEquals("recon-all -s session1 -all", outputFileContents[0]);

        final File fakeResourceDirFile = new File(fakeResourceDir);
        assertNotNull(fakeResourceDirFile);
        assertNotNull(fakeResourceDirFile.listFiles());
        final List<String> fakeResourceDirFileNames = Lists.newArrayList();
        for (final File file : fakeResourceDirFile.listFiles()) {
            fakeResourceDirFileNames.add(file.getName());

        }
        assertEquals(fakeResourceDirFileNames, Lists.newArrayList(outputFileContents[1].split(" ")));
    }
}
