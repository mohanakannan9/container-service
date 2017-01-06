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
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ContainerExecutionOutput;
import org.nrg.containers.model.xnat.XnatModelObject;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.services.archive.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerFinalizeHelper {
    private static final Logger log = LoggerFactory.getLogger(ContainerFinalizeHelper.class);

    private ContainerControlApi containerControlApi;
    private SiteConfigPreferences siteConfigPreferences;
    private TransportService transportService;
    private PermissionsServiceI permissionsService;
    private CatalogService catalogService;
    private ObjectMapper mapper;

    private ContainerExecution containerExecution;
    private UserI userI;
    private String exitCode;

    private Map<String, ContainerExecutionMount> untransportedMounts;
    private Map<String, ContainerExecutionMount> transportedMounts;
    private Map<String, XnatModelObject> inputCache;

    private String prefix;

    private ContainerFinalizeHelper(final ContainerExecution containerExecution,
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

        this.containerExecution = containerExecution;
        this.userI = userI;
        this.exitCode = exitCode;

        untransportedMounts = Maps.newHashMap();
        transportedMounts = Maps.newHashMap();
        inputCache = Maps.newHashMap();

        prefix = "Container " + containerExecution.getId() + ": ";
    }

    public static void finalizeContainer(final ContainerExecution containerExecution,
                                         final UserI userI,
                                         final String exitCode,
                                         final ContainerControlApi containerControlApi,
                                         final SiteConfigPreferences siteConfigPreferences,
                                         final TransportService transportService,
                                         final PermissionsServiceI permissionsService,
                                         final CatalogService catalogService,
                                         final ObjectMapper mapper) {
        final ContainerFinalizeHelper helper =
                new ContainerFinalizeHelper(containerExecution, userI, exitCode, containerControlApi, siteConfigPreferences, transportService, permissionsService, catalogService, mapper);
        helper.finalizeContainer();
    }

    private void finalizeContainer() {
        containerExecution.addLogPaths(uploadLogs());

        // TODO Add some stuff with status code. "x" means "don't know", "0" success, greater than 0 failure.

        if (containerExecution.getOutputs() != null) {
            if (containerExecution.getMountsOut() != null) {
                for (final ContainerExecutionMount mountOut : containerExecution.getMountsOut()) {
                    untransportedMounts.put(mountOut.getName(), mountOut);
                }
            }

            uploadOutputs();
        }
    }

    private Set<String> uploadLogs() {
        log.info(prefix + "Getting logs.");
        final Set<String> logPaths = Sets.newHashSet();

        String stdoutLogStr = "";
        String stderrLogStr = "";
        try {
            stdoutLogStr = containerControlApi.getContainerStdoutLog(containerExecution.getContainerId());
        } catch (DockerServerException | NoServerPrefException e) {
            log.error(prefix + "Could not get logs.", e);
        }
        try {
            stderrLogStr = containerControlApi.getContainerStderrLog(containerExecution.getContainerId());
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

    private void uploadOutputs() {
        log.info(prefix + "Uploading outputs.");


        for (final ContainerExecutionOutput output: containerExecution.getOutputs()) {
//            XnatModelObject created = null;
            try {
                output.setCreated(uploadOutput(output));
            } catch (ContainerException | RuntimeException e) {
                log.error("Cannot upload files for command output " + output.getName(), e);
            }
//            if (created != null) {
//                try {
//                    output.setCreated(mapper.writeValueAsString(created));
//                } catch (JsonProcessingException e) {
//                    if (log.isErrorEnabled()) {
//                        String message = prefix;
//                        message += String.format("Files for output \"%s\" were uploaded and saved, but the JSON representation of the object thus created could not be recorded.", output.getName());
//                        if (log.isDebugEnabled()) {
//                            message += "\n" + created;
//                        }
//                        log.error(message, e);
//                    }
//                }
//            }
        }

        log.info(prefix + "Done uploading outputs.");
    }

    private String uploadOutput(final ContainerExecutionOutput output) throws ContainerException {
        if (log.isInfoEnabled()) {
            log.info(String.format(prefix + "Uploading output \"%s\".", output.getName()));
        }
        if (log.isDebugEnabled()) {
            log.debug(output.toString());
        }

        final String mountName = output.getMount();
        final String relativeFilePath = output.getPath() != null ? output.getPath() : "";
        final ContainerExecutionMount mount = getMount(mountName);
        if (mount == null) {
            throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format(prefix + "Output files are provided by mount \"%s\": %s", mount.getName(), mount));
        }

        if (StringUtils.isBlank(mount.getHostPath())) {
            throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Mount \"%s\" has blank hostPath.", output.getName(), mount.getName()));
        }

        final List<File> toUpload;
        if (StringUtils.isBlank(relativeFilePath)) {
            // This is fine. It just means upload everything in the build directory.
            final File buildDir = new File(mount.getHostPath());
            final File[] buildDirContents = buildDir.listFiles();
            if (buildDirContents == null || buildDirContents.length == 0) {
                throw new ContainerException(String.format(prefix + "Nothing to upload for output \"%s\". Mount \"%s\" hostPath has no files.", output.getName(), mount.getName()));
            }
            toUpload = Arrays.asList(buildDirContents);
        } else {
            final String filePath = FilenameUtils.concat(mount.getHostPath(), relativeFilePath);
            if (StringUtils.isBlank(filePath)) {
                // This shouldn't happen. We know mount.getHostPath() and relativeFilePath are non-blank
                throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Mount \"%s\" hostPath + output path is blank.", output.getName(), mount.getName()));
            }

            toUpload = Lists.newArrayList(new File(filePath));
        }

        final String label = StringUtils.isNotBlank(output.getLabel()) ? output.getLabel() :
                StringUtils.isNotBlank(mount.getResource()) ? mount.getResource() :
                        mountName;

        String parentUri = getInputValue(output.getParentInputName());
        if (parentUri == null) {
            throw new ContainerException(String.format(prefix + "Cannot upload output \"%s\". Could not instantiate parent input \"%s\".", output.getName(), output.getParentInputName()));
        }
        if (!parentUri.startsWith("/archive")) {
            parentUri = "/archive" + parentUri;
        }

//        XnatModelObject created = null;
        String createdUri = null;
        switch (output.getType()) {
            case RESOURCE:
                if (log.isDebugEnabled()) {
                    final String template = prefix + "Inserting file resource.\n\tuser: %s\n\tparentUri: %s\n\tlabel: %s\n\ttoUpload: %s";
                    log.debug(String.format(template, userI.getLogin(), parentUri, label, toUpload));
                }
                final XnatResourcecatalog resourcecatalog;
                try {
                     resourcecatalog = catalogService.insertResources(userI, parentUri, toUpload, label, null, null, null);
                } catch (Exception e) {
                    throw new ContainerException(prefix + "Could not upload files to resource.", e);
                }

                createdUri = UriParserUtils.getArchiveUri(resourcecatalog);
                if (StringUtils.isBlank(createdUri)) {
                    createdUri = parentUri + "/resources/" + resourcecatalog.getLabel();
                }
                break;
            case ASSESSOR:
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
                break;
        }


        log.info(String.format(prefix + "Done uploading output \"%s\". URI of created output: %s", output.getName(), createdUri));
        return createdUri;
    }

    private ContainerExecutionMount getMount(final String mountName) throws ContainerException {
        // If mount has been transported, we're done
        if (transportedMounts.containsKey(mountName)) {
            return transportedMounts.get(mountName);
        }

        // If mount exists but has not been transported, transport it
        if (untransportedMounts.containsKey(mountName)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "Transporting mount \"%s\".", mountName));
            }
            final ContainerExecutionMount mountToTransport = untransportedMounts.get(mountName);
            final Path pathOnExecutionMachine = Paths.get(mountToTransport.getHostPath());
            final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing
            mountToTransport.setHostPath(pathOnXnatMachine.toAbsolutePath().toString());

            transportedMounts.put(mountName, mountToTransport);
            untransportedMounts.remove(mountName);

            log.debug(prefix + "Done transporting mount.");
            return mountToTransport;
        }

        // Mount does not exist
        throw new ContainerException(String.format(prefix + "Mount \"%s\" does not exist.", mountName));
    }

    private String getInputValue(final String inputName) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(prefix + "Getting URI for input \"%s\".", inputName));
        }
//        if (inputCache.containsKey(inputName)) {
//            if (log.isDebugEnabled()) {
//                log.debug(prefix + "Input was cached.");
//            }
//            return inputCache.get(inputName);
//        }

        final Map<String, String> inputValues = containerExecution.getInputValues();
        if (!inputValues.containsKey(inputName)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(prefix + "No input found with name \"%s\". Input name set: %s", inputName, inputValues.keySet()));
            }
            return null;
        }

        return inputValues.get(inputName);
//        try {
//            final XnatModelObject input = mapper.readValue(inputValue, XnatModelObject.class);
//            if (log.isDebugEnabled()) {
//                log.debug(String.format(prefix + "Caching input \"%s\": %s", inputName, input));
//            } else if (log.isInfoEnabled()){
//                log.info(String.format(prefix + "Caching input \"%s\".", inputName));
//            }
//
//            inputCache.put(inputName, input);
//            return input;
//        } catch (IOException e) {
//            if (log.isDebugEnabled()) {
//                // Yes, I know I checked for "debug" and am logging at "error".
//                // I still want this to show up as "error" either way, but I only want the full object to
//                // be logged if you opted into the firehose.
//                log.error(prefix + "Could not deserialize input value:\n" + inputValue, e);
//            } else {
//                log.error(prefix + "Could not deserialize input value.", e);
//            }
//        }
//
//        return null;
    }
}
