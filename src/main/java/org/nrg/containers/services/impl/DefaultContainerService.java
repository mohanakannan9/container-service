package org.nrg.containers.services.impl;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.nrg.automation.entities.Script;
import org.nrg.automation.services.ScriptService;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoHubException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.services.ContainerService;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Service
public class DefaultContainerService implements ContainerService {
    private static final Logger _log = LoggerFactory.getLogger(DefaultContainerService.class);

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private ContainerControlApi controlApi;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private ScriptService scriptService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private TransportService transportService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private SiteConfigPreferences preferences;

//    public List<DockerImage> getAllImages() throws NoServerPrefException, DockerServerException {
//        return controlApi.getAllImages();
//    }
//
//    public DockerImage getImageByName(final String name) throws NoServerPrefException, NotFoundException, DockerServerException {
//        return controlApi.getImageByName(name);
//    }
//
//    @Override
//    public DockerImage getImageById(String id) throws NoServerPrefException, NotFoundException, DockerServerException {
//        // TODO Figure out what to do with this. Not sure if we need to be fetching images by id.
//        return controlApi.getImageById(id);
//    }
//
//    @Override
//    public String deleteImageById(final String id, final Boolean onServer) throws NoServerPrefException, NotFoundException, DockerServerException {
//        controlApi.deleteImageById(id);
//        // TODO delete image in XNAT database
//        return null;
//    }
//
//    @Override
//    public String deleteImageByName(final String name, final Boolean onServer) throws NoServerPrefException, NotFoundException, DockerServerException {
//        controlApi.deleteImageByName(name);
//        // TODO delete imaage in XNAT database
//        return null;
//    }

    @Override
    public List<Container> getContainers() throws NoServerPrefException, DockerServerException {
        return controlApi.getAllContainers();
    }

    public String getContainerStatus(final String id) throws NoServerPrefException, NotFoundException, DockerServerException {
        return controlApi.getContainerStatus(id);
    }

    public Container getContainer(final String id) throws NoServerPrefException, NotFoundException, DockerServerException {
        return controlApi.getContainer(id);
    }

    @Override
    public String launch(final String imageName, final Map<String, String> params, Boolean wait)
            throws NoServerPrefException, NotFoundException, DockerServerException {

        // Find image and metadata
        // Resolve all arguments
        // stage data
        // launch
        //return controlApi.launchImage(server(), imageName, params.getCommandArray(), params.getVolumesArray());
        return null;
    }

    @Override
    public String launchOn(final String imageName, final String xnatId, final String type, final Map<String, String> launchArguments, final Boolean wait) {
        // Find xnat object
        // Find image and metadata
        // Resolve all arguments
        // stage data
        // launch
        return null;
    }

    @Override
    public String launchFromScript(String scriptId, Map<String, String> launchArguments, Boolean wait)
        throws Exception {
        final Script script = scriptService.getByScriptId(scriptId);
        if (script == null) {
            throw new NotFoundException("Could not find script " + scriptId);
        }

        final String context = script.getLanguage();
//        final ImageMetadata metadata = imageMetadataService.getMetadataFromContext(context);
//        final String imageId = metadata.getImageId();

        // TODO Remove all the hard-coded stuff from sprint 4. Make this work generically.
//        List<ImageMetadataArg> args = metadata.getArgs();
        // String description = metadata.getDecription();
//        String execution = metadata.getExecution();
//        String imageId = metadata.getImageId();
//        List<String> mountsIn = Lists.newArrayList(metadata.getMountsIn());
//        List<String> mountsOut = Lists.newArrayList(metadata.getMountsOut());
        //String type = metadata.getType();

//        String sessionId = null;
//        XFTItem session = null;
//        for (final ImageMetadataArg arg : args) {
//            if (arg.getType().startsWith("xnat")) {
//                final String id = launchArguments.get(arg.getValue());
//                if (id == null || id.equals("")) {
//                    throw new Exception(
//                        String.format("Input argument %s is required, must contain a %s ID.",
//                            arg.getValue(), arg.getType()));
//                }
//
//                sessionId = id;
//                session = ItemSearch.GetItem(arg.getType() + ".id", id, Users.getUser("admin"), false);
//                launchArguments.remove(arg.getValue());
//                break;
//            }
//        }
//        if (sessionId == null) {
//            sessionId = "";
//        }
//        final String sessionId = launchArguments.get("sessionId");
//        final XFTItem session = ItemSearch.GetItem("xnat:mrSessionData.id", sessionId, Users.getUser("admin"), false);
//        final String mountIn = launchArguments.get("mountIn");
//        final String mountOut = launchArguments.get("mountOut");
//        final String imageId = launchArguments.get("imageId");
//        final String host = launchArguments.get("host");

        // Resolve args from launchArguments


        // Transport files
        final String server = controlApi.getServer().getHost();
//        final List<Path> paths = transportService.transport(server, session);

        final Calendar cal = Calendar.getInstance();
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        final String timestamp = formatter.format(cal.getTime());
        final String buildDir = preferences.getBuildPath() + timestamp;

        final String filename = "script";
        final File filenameWithPath = new File(buildDir, filename);
        FileUtils.writeStringToFile(filenameWithPath, script.getContent());
        final Path scriptPath = transportService.transport(server, filenameWithPath.toPath());

//        final List<String> command = Lists.newArrayList("python", "/data/output/"+filename,
//            "-h", host, "-u", "admin", "-p", "admin",
//            "-s", sessionId);
        final List<String> command = Lists.newArrayList();
//        final List<String> volumes = Lists.newArrayList(
//            String.format("%s:%s", paths.get(0), mountIn),
//            String.format("%s:%s", buildDir, mountOut));
        final List<String> volumes = Lists.newArrayList();
//        return controlApi.launchImage(imageId, command, volumes);
        return null;
    }


    @Override
    public String getContainerLogs(final String id)
        throws NoServerPrefException, NotFoundException, DockerServerException {
        return controlApi.getContainerLogs(id);
    }

    @Override
    public String verbContainer(final String id, final String status)
            throws NoServerPrefException, NotFoundException, DockerServerException {
        // TODO
        return null;
    }

//    @Override
//    public void pullByName(final String image, final String hub,
//                           final String hubUsername, final String hubPassword)
//        throws NoHubException, NotFoundException, DockerServerException, IOException, NoServerPrefException {
//        final DockerHub hubWithAuth = DockerHub.builder()
//            .url(hub)
//            .username(hubUsername)
//            .password(hubPassword)
//            .build();
//        pullByName(image, hubWithAuth);
//    }
//
//    @Override
//    public void pullByName(String image, String hub)
//        throws NoHubException, NotFoundException, DockerServerException, IOException, NoServerPrefException {
//        final DockerHub hubNoAuth = DockerHub.builder().url(hub).build();
//        pullByName(image, hubNoAuth);
//    }

//    private void pullByName(String image, DockerHub hub)
//        throws NoHubException, NotFoundException, DockerServerException, IOException, NoServerPrefException {
//        controlApi.pullImage(image, hub);
//    }

    @Override
    public DockerImage pullFromSource(final String source, final String name)
            throws NoHubException, NotFoundException, DockerServerException {
        // TODO
        return null;
    }

//    @Override
//    public void setMetadataByName(final String imageName, final ImageMetadata metadata, final String project,
//                                  final Boolean overwrite, final Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        final DockerImage image = getImageByName(imageName);
//        imageMetadataService.setMetadata(image, metadata, project, overwrite, ignoreBlank);
//    }
//
//    @Override
//    public void setMetadataById(final String imageId, final ImageMetadata metadata, final String project,
//                                final Boolean overwrite, final Boolean ignoreBlank)
//            throws NoServerPrefException, NotFoundException, DockerServerException {
//        final DockerImage image = getImageById(imageId);
//        imageMetadataService.setMetadata(image, metadata, project, overwrite, ignoreBlank);
//    }

    @Override
    public String setMetadataById(String id, Map<String, String> metadata, String project, Boolean overwrite, Boolean ignoreBlank) throws NoServerPrefException, NotFoundException {
        return null;
    }
}
