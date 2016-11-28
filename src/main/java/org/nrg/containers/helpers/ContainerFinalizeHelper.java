package org.nrg.containers.helpers;

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.CommandInput;
import org.nrg.containers.model.CommandMount;
import org.nrg.containers.model.CommandOutput;
import org.nrg.containers.model.CommandOutputFiles;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.services.PermissionsServiceI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.services.archive.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ContainerFinalizeHelper {
    private static final Logger log = LoggerFactory.getLogger(ContainerFinalizeHelper.class);

    private ContainerControlApi containerControlApi;
    private SiteConfigPreferences siteConfigPreferences;
    private TransportService transportService;
    private PermissionsServiceI permissionsService;
    private CatalogService catalogService;

    private ContainerExecution containerExecution;
    private UserI userI;

    private Map<String, CommandMount> untransportedMounts;
    private Map<String, CommandMount> transportedMounts;

    private ContainerFinalizeHelper(final ContainerExecution containerExecution,
                                    final UserI userI,
                                    final ContainerControlApi containerControlApi,
                                    final SiteConfigPreferences siteConfigPreferences,
                                    final TransportService transportService,
                                    final PermissionsServiceI permissionsService,
                                    final CatalogService catalogService) {
        this.containerControlApi = containerControlApi;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transportService = transportService;
        this.permissionsService = permissionsService;
        this.catalogService = catalogService;

        this.containerExecution = containerExecution;
        this.userI = userI;

        untransportedMounts = Maps.newHashMap();
        transportedMounts = Maps.newHashMap();
    }

    public static void finalizeContainer(final ContainerExecution containerExecution,
                                         final UserI userI,
                                         final ContainerControlApi containerControlApi,
                                         final SiteConfigPreferences siteConfigPreferences,
                                         final TransportService transportService,
                                         final PermissionsServiceI permissionsService,
                                         final CatalogService catalogService) {
        final ContainerFinalizeHelper helper =
                new ContainerFinalizeHelper(containerExecution, userI, containerControlApi, siteConfigPreferences, transportService, permissionsService, catalogService);
        helper.finalizeContainer();
    }

    private void finalizeContainer() {
        uploadLogs();

        if (containerExecution.getOutputs() != null) {
            if (containerExecution.getMountsOut() != null) {
                for (final CommandMount mountOut : containerExecution.getMountsOut()) {
                    untransportedMounts.put(mountOut.getName(), mountOut);
                }
            }

            uploadOutputs();
        }
    }

    private void uploadLogs() {
        if (log.isDebugEnabled()) {
            log.debug("Getting container logs");
        }

        String stdoutLogStr = "";
        String stderrLogStr = "";
        try {
            stdoutLogStr = containerControlApi.getContainerStdoutLog(containerExecution.getContainerId());
            stderrLogStr = containerControlApi.getContainerStderrLog(containerExecution.getContainerId());
        } catch (DockerServerException | NoServerPrefException e) {
            log.error("Could not get container logs for container with id " + containerExecution.getContainerId(), e);
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

                if (log.isDebugEnabled()) {
                    log.debug("Saving container logs to " + destinationPath);
                }

                if (StringUtils.isNotBlank(stdoutLogStr)) {
                    final File stdoutFile = new File(destination, "stdout.log");
                    FileUtils.OutputToFile(stdoutLogStr, stdoutFile.getAbsolutePath());
                }

                if (StringUtils.isNotBlank(stderrLogStr)) {
                    final File stderrFile = new File(destination, "stderr.log");
                    FileUtils.OutputToFile(stderrLogStr, stderrFile.getAbsolutePath());
                }
            }
        }
    }

    private void uploadOutputs() {



        for (final CommandOutput output: containerExecution.getOutputs()) {
            try {
                uploadOutput(output);
            } catch (ContainerException e) {
                log.error("Cannot upload files for command output " + output.getName(), e);
            }
        }

//        for (final CommandMount mount : toUpload) {
//            if (StringUtils.isBlank(mount.getName())) {
//                log.error(String.format("Cannot upload mount for container execution %s. Mount has no resource name. %s",
//                        containerExecution.getId(), mount));
//                continue;
//            }
//            if (StringUtils.isBlank(mount.getHostPath())) {
//                log.error(String.format("Cannot upload mount for container execution %s. Mount has no path to files. %s",
//                        containerExecution.getId(), mount));
//                continue;
//            }
//            final Path pathOnExecutionMachine = Paths.get(mount.getHostPath());
//            final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing

//                        final HttpPost post = new HttpPost(url);
//                        post.setHeader("Authorization", "Basic " + encodedAuth);

//                        final HttpResponse response = client.execute(post, clientContext);
//                        final HttpResponse response = client.execute(post);

//                    try {
//                        final Response response = given().auth().preemptive().basic(token.getAlias(), token.getSecret()).when().post(url).andReturn();
//                        if (response.getStatusCode() > 400) {
//                            log.error(String.format("Upload failed for container execution %s, mount %s.\n" +
//                                            "Attempted POST %s.\nUpload returned response: %s",
//                                    containerExecution.getId(), mount, url, response.getStatusLine()));
//                        }
//                    } catch (Exception e) {
//                        log.error(String.format("Upload failed for container execution %s, mount %s.\n" +
//                                        "Attempted POST %s.\nGot an exception.",
//                                containerExecution.getId(), mount, url), e);
//                    }
                    // TODO Actually upload files as new resource


//                }

//        final String rootId = containerExecution.getRootObjectId();
//        final String rootXsiType = containerExecution.getRootObjectXsiType();
//        if (StringUtils.isNotBlank(rootXsiType) && StringUtils.isNotBlank(rootId)) {
//            final AliasToken token = aliasTokenService.issueTokenForUser(userI);
//
////            final String authToken = token.getAlias() + ":" + token.getSecret();
////            String encodedAuth = "";
////            try {
////                final byte[] authTokenBytes = authToken.getBytes("UTF-8");
////                final byte[] authTokenBase64Bytes = Base64.encode(authTokenBytes);
////                encodedAuth = new String(authTokenBase64Bytes, "UTF-8");
////            } catch (UnsupportedEncodingException e) {
////                log.error("Sorry, can't get an auth token", e);
////            }
//
//
////            final String xnatUrl = siteConfigPreferences.getSiteUrl();
//            final String xnatUrl = "http://localhost:80";
//            final HttpHost targetHost = new HttpHost(xnatUrl);
//            String url = xnatUrl + "/data";
//
//            if (rootXsiType.matches(".+?:.*?[Ss]can.*") && rootId.contains(":")) {
//                final String scanId = StringUtils.substringAfterLast(rootId, ":");
//                final String sessionId = StringUtils.substringBeforeLast(rootId, ":");
//
//                url += String.format("/experiments/%s/scans/%s", sessionId, scanId);
//            } else if (rootXsiType.matches(".+?:.*?[Ss]ubject.*")) {
//                url += String.format("/subjects/%s", rootId);
//            } else if (rootXsiType.matches(".+?:.*?[Pp]roject.*")) {
//                url += String.format("/projects/%s", rootId);
//            } else {
//                // If all else fails, it's an experiment
//                url += String.format("/experiments/%s", rootId);
//            }
//
////            final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
////            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
////            credentialsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), new UsernamePasswordCredentials(token.getAlias(), token.getSecret()));
////            final AuthCache authCache = new BasicAuthCache();
////            final BasicScheme basicAuth = new BasicScheme();
////            authCache.put(targetHost, basicAuth);
////            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
////            final HttpClientContext clientContext = new HttpClientContext();
////            clientContext.setAuthCache(authCache);
////            clientContext.setCredentialsProvider(credentialsProvider);
////            try (final CloseableHttpClient client = clientBuilder.build()) {
////            try (final CloseableHttpClient client = HttpClients.createDefault()) {
//                for (final CommandMount mount : toUpload) {
//                    if (StringUtils.isBlank(mount.getName())) {
//                        log.error(String.format("Cannot upload mount for container execution %s. Mount has no resource name. %s",
//                                containerExecution.getId(), mount));
//                        continue;
//                    }
//                    if (StringUtils.isBlank(mount.getHostPath())) {
//                        log.error(String.format("Cannot upload mount for container execution %s. Mount has no path to files. %s",
//                                containerExecution.getId(), mount));
//                        continue;
//                    }
//                    final Path pathOnExecutionMachine = Paths.get(mount.getHostPath());
//                    final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing
//                    url += String.format("/resources/%s/files", mount.getName());
//                    try {
//                        url += String.format("?overwrite=%s&reference=%s",
//                                URLEncoder.encode(mount.getOverwrite().toString(), UTF8),
//                                URLEncoder.encode(pathOnXnatMachine.toString(), UTF8));
//                    } catch (IOException e) {
//                        log.error(String.format("Cannot upload mount for container execution %s. There was an error. %s", containerExecution.getId(), mount), e);
//                    }
//
////                        final HttpPost post = new HttpPost(url);
////                        post.setHeader("Authorization", "Basic " + encodedAuth);
//
////                        final HttpResponse response = client.execute(post, clientContext);
////                        final HttpResponse response = client.execute(post);
//
////                    try {
////                        final Response response = given().auth().preemptive().basic(token.getAlias(), token.getSecret()).when().post(url).andReturn();
////                        if (response.getStatusCode() > 400) {
////                            log.error(String.format("Upload failed for container execution %s, mount %s.\n" +
////                                            "Attempted POST %s.\nUpload returned response: %s",
////                                    containerExecution.getId(), mount, url, response.getStatusLine()));
////                        }
////                    } catch (Exception e) {
////                        log.error(String.format("Upload failed for container execution %s, mount %s.\n" +
////                                        "Attempted POST %s.\nGot an exception.",
////                                containerExecution.getId(), mount, url), e);
////                    }
//                    // TODO Actually upload files as new resource
//
//
//                }
////            } catch (IOException e) {
////                log.error(String.format("Cannot upload files for container execution %s. Could not connect out and back to XNAT.", containerExecution.getId()));
////            }
//        } else {
//            // No root id and/or xsi type, so I can't upload anything.
//            // If there is anything there that I am supposed to upload, I will log that fact.
//            if (toUpload != null && !toUpload.isEmpty()) {
//                log.error("Cannot upload outputs for container execution " + containerExecution.getId() + ". No root id and/or xsi type.");
//            }
//        }
    }

    private void uploadOutput(final CommandOutput output) throws ContainerException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Uploading command output \"%s\".", output.getName()));
        }

        final CommandOutputFiles filesObj = output.getFiles();
        if (output.getFiles() == null) {
            throw new ContainerException(String.format("Command output \"%s\" has no files.", output.getName()));
        }

        final String mountName = filesObj.getMount();
        final String relativeFilePath = filesObj.getPath();
        final CommandMount mount = getMount(mountName);
        if (mount == null) {
            throw new ContainerException(String.format("Mount \"%s\" does not exist.", mountName));
        }
        final String absoluteFilePath = FilenameUtils.concat(mount.getHostPath(), relativeFilePath);
        final File outputFile = new File(absoluteFilePath);

        final String label = StringUtils.isNotBlank(output.getLabel()) ? output.getLabel() :
                StringUtils.isNotBlank(mount.getResource()) ? mount.getResource() :
                        mountName;

        final String parentUri = getParentUri(output.getParent());

        switch (output.getType()) {
            case RESOURCE:
                try {
                    catalogService.insertResources(userI, parentUri, outputFile, label, null, null, null);
                } catch (Exception e) {
                    throw new ContainerException("Could not upload files to resource.", e);
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
    }

    private CommandMount getMount(final String mountName) throws ContainerException {
        // If mount has been transported, we're done
        if (transportedMounts.containsKey(mountName)) {
            return transportedMounts.get(mountName);
        }

        // If mount exists but has not been transported, transport it
        if (untransportedMounts.containsKey(mountName)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Transporting mount \"%s\".", mountName));
            }
            final CommandMount mountToTransport = untransportedMounts.get(mountName);
            final Path pathOnExecutionMachine = Paths.get(mountToTransport.getHostPath());
            final Path pathOnXnatMachine = transportService.transport("", pathOnExecutionMachine); // TODO this currently does nothing
            mountToTransport.setHostPath(pathOnXnatMachine.toAbsolutePath().toString());

            transportedMounts.put(mountName, mountToTransport);
            untransportedMounts.remove(mountName);
            return mountToTransport;
        }

        // Mount does not exist
        throw new ContainerException(String.format("Mount \"%s\" does not exist.", mountName));
    }

    private String getParentUri(final String parentInputName) {
        return "";
    }
}
