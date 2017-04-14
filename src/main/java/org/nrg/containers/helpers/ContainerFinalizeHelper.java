package org.nrg.containers.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.containers.model.container.entity.ContainerEntityOutput;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.services.archive.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.nrg.containers.model.command.entity.CommandWrapperOutputEntity.Type.ASSESSOR;
import static org.nrg.containers.model.command.entity.CommandWrapperOutputEntity.Type.RESOURCE;

public class ContainerFinalizeHelper {
    private static final Logger log = LoggerFactory.getLogger(ContainerFinalizeHelper.class);

    private ContainerControlApi containerControlApi;
    private SiteConfigPreferences siteConfigPreferences;
    private TransportService transportService;
    private PermissionsServiceI permissionsService;
    private CatalogService catalogService;
    private ObjectMapper mapper;

    private ContainerEntity containerEntity;
    private UserI userI;
    private String exitCode;

    private Map<String, ContainerEntityMount> untransportedMounts;
    private Map<String, ContainerEntityMount> transportedMounts;

    private String prefix;

    private ContainerFinalizeHelper(final ContainerEntity containerEntity,
                                    final UserI userI,
                                    final String exitCode,
                                    final ContainerControlApi containerControlApi,
                                    final SiteConfigPreferences siteConfigPreferences,
                                    final TransportService transportService,
                                    final PermissionsServiceI permissionsService,
                                    final CatalogService catalogService,
                                    final ObjectMapper mapper) {
        this.containerControlApi = containerControlApi;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transportService = transportService;
        this.permissionsService = permissionsService;
        this.catalogService = catalogService;
        this.mapper = mapper;

        this.containerEntity = containerEntity;
        this.userI = userI;
        this.exitCode = exitCode;

        untransportedMounts = Maps.newHashMap();
        transportedMounts = Maps.newHashMap();

        prefix = "Container " + containerEntity.getId() + ": ";
    }

    public static void finalizeContainer(final ContainerEntity containerEntity,
                                         final UserI userI,
                                         final String exitCode,
                                         final ContainerControlApi containerControlApi,
                                         final SiteConfigPreferences siteConfigPreferences,
                                         final TransportService transportService,
                                         final PermissionsServiceI permissionsService,
                                         final CatalogService catalogService,
                                         final ObjectMapper mapper) {
        final ContainerFinalizeHelper helper =
                new ContainerFinalizeHelper(containerEntity, userI, exitCode, containerControlApi, siteConfigPreferences, transportService, permissionsService, catalogService, mapper);
        helper.finalizeContainer();
    }

    private void finalizeContainer() {
        containerEntity.addLogPaths(uploadLogs());

        // TODO Add some stuff with status code. "x" means "don't know", "0" success, greater than 0 failure.

        if (containerEntity.getOutputs() != null) {
            if (containerEntity.getMounts() != null) {
                for (final ContainerEntityMount mountOut : containerEntity.getMounts()) {
                    untransportedMounts.put(mountOut.getName(), mountOut);
                }
            }

            final List<Exception> failedRequiredOutputs = uploadOutputs();
            if (!failedRequiredOutputs.isEmpty()) {
                // TODO this means a required output was not uploaded. Mark the execution as "failed" with an appropriate status message.
            }
        }
    }

    private Set<String> uploadLogs() {
        log.info(prefix + "Getting logs.");
        final Set<String> logPaths = Sets.newHashSet();

        String stdoutLogStr = "";
        String stderrLogStr = "";
        try {
            stdoutLogStr = containerControlApi.getContainerStdoutLog(containerEntity.getContainerId());
        } catch (DockerServerException | NoServerPrefException e) {
            log.error(prefix + "Could not get logs.", e);
        }
        try {
            stderrLogStr = containerControlApi.getContainerStderrLog(containerEntity.getContainerId());
        } catch (DockerServerException | NoServerPrefException e) {
            log.error(prefix + "Could not get logs.", e);
        }

        if (StringUtils.isNotBlank(stdoutLogStr) || StringUtils.isNotBlank(stderrLogStr)) {

            final String archivePath = siteConfigPreferences.getArchivePath(); // TODO find a place to upload this thing. Root of the archive if sitewide, else under the archive path of the root object
            if (StringUtils.isNotBlank(archivePath)) {
                final SimpleDateFormat formatter = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
                final String datestamp = formatter.format(new Date());
                final String containerExecPath = FileUtils.AppendRootPath(archivePath, "CONTAINER_EXEC/");
                final String destinationPath = containerExecPath + datestamp + "/LOGS/";
                final File destination = new File(destinationPath);
                destination.mkdirs();

                log.info(prefix + "Saving logs to " + destinationPath);

                if (StringUtils.isNotBlank(stdoutLogStr)) {
                    log.debug("Saving stdout");
                    final File stdoutFile = new File(destination, "stdout.log");
                    FileUtils.OutputToFile(stdoutLogStr, stdoutFile.getAbsolutePath());
                    logPaths.add(stdoutFile.getAbsolutePath());
                } else {
                    log.debug("Stdout was blank");
                }

                if (StringUtils.isNotBlank(stderrLogStr)) {
                    log.debug("Saving stderr");
                    final File stderrFile = new File(destination, "stderr.log");
                    FileUtils.OutputToFile(stderrLogStr, stderrFile.getAbsolutePath());
                    logPaths.add(stderrFile.getAbsolutePath());
                } else {
                    log.debug("Stderr was blank");
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Adding log paths to container");
        }
        return logPaths;
    }

    private List<Exception> uploadOutputs() {
        log.info(prefix + "Uploading outputs.");

        final List<Exception> failedRequiredOutputs = Lists.newArrayList();
        for (final ContainerEntityOutput output: containerEntity.getOutputs()) {
            try {
                output.setCreated(uploadOutput(output));
            } catch (ContainerException | RuntimeException e) {
                log.error("Cannot upload files for command output " + output.getName(), e);
                if (output.isRequired()) {
                    failedRequiredOutputs.add(e);
                }
            }
        }

        log.info(prefix + "Done uploading outputs.");
        return failedRequiredOutputs;
    }

    private String uploadOutput(final ContainerEntityOutput output) throws ContainerException {
        if (log.isInfoEnabled()) {
            log.info(String.format(prefix + "Uploading output \"%s\".", output.getName()));
        }
        if (log.isDebugEnabled()) {
            log.debug(output.toString());
        }

        final String mountName = output.getMount();
        final ContainerEntityMount mount = getMount(mountName);
        if (mount == null) {
            throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format(prefix + "Output files are provided by mount \"%s\": %s", mount.getName(), mount));
        }

        final String mountXnatHostPath = mount.getXnatHostPath();
        if (StringUtils.isBlank(mountXnatHostPath)) {
            throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Mount \"%s\" has a blank path to the files on the XNAT machine.", output.getName(), mount.getName()));
        }

        final String relativeFilePath = output.getPath() != null ? output.getPath() : "";
        final String filePath = StringUtils.isBlank(relativeFilePath) ? mountXnatHostPath :
                FilenameUtils.concat(mountXnatHostPath, relativeFilePath);
        final String globMatcher = output.getGlob() != null ? output.getGlob() : "";

        final List<File> toUpload = matchGlob(filePath, globMatcher);
        if (toUpload == null || toUpload.size() == 0) {
            if (output.isRequired()) {
                throw new ContainerException(String.format(prefix + "Nothing to upload for output \"%s\". Mount \"%s\" has no files.", output.getName(), mount.getName()));
            }
            return "";
        }

        final String label = StringUtils.isNotBlank(output.getLabel()) ? output.getLabel() : mountName;

        String parentUri = getWrapperInputValue(output.getHandledByXnatCommandInput());
        if (parentUri == null) {
            throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Could not instantiate object from input \"%s\".", output.getName(), output.getHandledByXnatCommandInput()));
        }
        if (!parentUri.startsWith("/archive")) {
            parentUri = "/archive" + parentUri;
        }

        String createdUri = null;
        final String type = output.getType();
        if (type.equals(RESOURCE.getName())) {
            if (log.isDebugEnabled()) {
                final String template = prefix + "Inserting file resource.\n\tuser: %s\n\tparentUri: %s\n\tlabel: %s\n\ttoUpload: %s";
                log.debug(String.format(template, userI.getLogin(), parentUri, label, toUpload));
            }

            try {
                final XnatResourcecatalog resourcecatalog = catalogService.insertResources(userI, parentUri, toUpload, label, null, null, null);
                createdUri = UriParserUtils.getArchiveUri(resourcecatalog);
                if (StringUtils.isBlank(createdUri)) {
                    createdUri = parentUri + "/resources/" + resourcecatalog.getLabel();
                }
            } catch (Exception e) {
                throw new ContainerException(prefix + "Could not upload files to resource.", e);
            }
        } else if (type.equals(ASSESSOR.getName())) {
            /* TODO Waiting on XNAT-4556
            final CommandMount mount = getMount(output.getFiles().getMount());
            final String absoluteFilePath = FilenameUtils.concat(mount.getHostPath(), output.getFiles().getPath());
            final SAXReader reader = new SAXReader(userI);
            XFTItem item = null;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Reading XML file at " + absoluteFilePath);
                }
                item = reader.parse(new File(absoluteFilePath));

            } catch (IOException e) {
                log.error("An error occurred reading the XML", e);
            } catch (SAXException e) {
                log.error("An error occurred parsing the XML", e);
            }

            if (!reader.assertValid()) {
                throw new ContainerException("XML file invalid", reader.getErrors().get(0));
            }
            if (item == null) {
                throw new ContainerException("Could not create assessor from XML");
            }

            try {
                if (item.instanceOf("xnat:imageAssessorData")) {
                    final XnatImageassessordata assessor = (XnatImageassessordata) BaseElement.GetGeneratedItem(item);
                    if(permissionsService.canCreate(userI, assessor)){
                        throw new ContainerException(String.format("User \"%s\" has insufficient privileges for assessors in project \"%s\".", userI.getLogin(), assessor.getProject()));
                    }

                    if(assessor.getLabel()==null){
                        assessor.setLabel(assessor.getId());
                    }

                    // I hate this
                }
            } catch (ElementNotFoundException e) {
                throw new ContainerException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
             */
        }


        log.info(String.format(prefix + "Done uploading output \"%s\". URI of created output: %s", output.getName(), createdUri));
        return createdUri;
    }

    private ContainerEntityMount getMount(final String mountName) throws ContainerException {
        // If mount has been transported, we're done
        if (transportedMounts.containsKey(mountName)) {
            return transportedMounts.get(mountName);
        }

        // If mount exists but has not been transported, transport it
        if (untransportedMounts.containsKey(mountName)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "Transporting mount \"%s\".", mountName));
            }
            final ContainerEntityMount mountToTransport = untransportedMounts.get(mountName);

            if (StringUtils.isBlank(mountToTransport.getXnatHostPath())) {
                final Path pathOnExecutionMachine = Paths.get(mountToTransport.getContainerHostPath());
                final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing
                mountToTransport.setXnatHostPath(pathOnXnatMachine.toAbsolutePath().toString());
            } else {
                // TODO add transporter method to transport from specified source path to specified destination path
                // transporter.transport(sourceMachineName, mountToTransport.getContainerHostPath(), mountToTransport.getXnatHostPath());
            }

            transportedMounts.put(mountName, mountToTransport);
            untransportedMounts.remove(mountName);

            log.debug(prefix + "Done transporting mount.");
            return mountToTransport;
        }

        // Mount does not exist
        throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
    }

    private String getWrapperInputValue(final String inputName) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(prefix + "Getting URI for input \"%s\".", inputName));
        }

        final Map<String, String> wrapperInputs = containerEntity.getWrapperInputs();
        if (!wrapperInputs.containsKey(inputName)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "No input found with name \"%s\". Input name set: %s", inputName, wrapperInputs.keySet()));
            }
            return null;
        }

        return wrapperInputs.get(inputName);
    }

    private List<File> matchGlob(final String rootPath, final String glob) {
        final File rootDir = new File(rootPath);
        final File[] files = rootDir.listFiles();
        return files == null ? Lists.<File>newArrayList() : Arrays.asList(files);
    }
}