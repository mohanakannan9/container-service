package org.nrg.containers.services.impl;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.nrg.automation.services.ScriptService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoHubException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerHub;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private ImageMetadataService imageMetadataService;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private ScriptService scriptService;

    

    public List<Image> getAllImages() throws NoServerPrefException {
        return controlApi.getAllImages();
    }

    public Image getImageByName(final String name) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getImageByName(name);
    }

    @Override
    public Image getImageById(String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO Figure out what to do with this. Not sure if we need to be fetching images by id.
        return controlApi.getImageById(id);
    }

    @Override
    public String deleteImageById(final String id, final Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public String deleteImageByName(final String name, final Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public List<Container> getContainers(final Map<String, List<String>> params) throws NoServerPrefException, ContainerServerException {
        // TODO do stuff with queryParams
        return controlApi.getAllContainers();
    }

    public String getContainerStatus(final String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getContainerStatus(id);
    }

    public Container getContainer(final String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getContainer(id);
    }

    @Override
    public String launch(final String imageName, final Map<String, String> params, Boolean wait)
            throws NoServerPrefException, NotFoundException, ContainerServerException {

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
    public String launchFromScript(String scriptId, Map<String, String> launchArguments, Boolean wait) {
        final String context = scriptService.getByScriptId(scriptId).getLanguage();
        //TODO final ImageMetadata metadata = imageMetadataService.getByScriptingContext(context);
        final ImageMetadata metadata = null;

        // Pull args from metadata
        // Resolve args from launchArguments
        // Use transporter to stage any files


        final String imageId = null; // metadata.getImageId();
        final String[] command = null;
        final String[] volumes = null;
        return controlApi.launchImage(imageId, command, volumes);
    }

    @Override
    public String getContainerLogs(final String id)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public String verbContainer(final String id, final String status)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public ContainerHub getHub(final String hub, final Boolean verbose) throws NotFoundException {
        // TODO
        return null;
    }

    @Override
    public List<ContainerHub> getHubs(final Boolean verbose) throws NotFoundException {
        // TODO
        return null;
    }

    @Override
    public void setHub(final ContainerHub hub, final Boolean overwrite, final Boolean ignoreBlank) {
        // TODO
    }

    @Override
    public String search(final String term) throws NoHubException {
        // TODO
        return null;
    }

    @Override
    public Image pullByName(final String image, final String hub, final String name)
            throws NoHubException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public Image pullFromSource(final String source, final String name)
            throws NoHubException, NotFoundException, ContainerServerException {
        // TODO
        return null;
    }

    @Override
    public void setMetadataByName(final String imageName, final ImageMetadata metadata, final String project,
                                  final Boolean overwrite, final Boolean ignoreBlank)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        final Image image = getImageByName(imageName);
        imageMetadataService.setMetadata(image, metadata, project, overwrite, ignoreBlank);
    }

    @Override
    public void setMetadataById(final String imageId, final ImageMetadata metadata, final String project,
                                final Boolean overwrite, final Boolean ignoreBlank)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        final Image image = getImageById(imageId);
        imageMetadataService.setMetadata(image, metadata, project, overwrite, ignoreBlank);
    }

    @Override
    public ContainerServer getServer() throws NoServerPrefException, NotFoundException {
        return controlApi.getServer();
    }

    @Override
    public void setServer(String host) throws InvalidPreferenceName {
        // TODO
    }

    @Override
    public String setMetadataById(String id, Map<String, String> metadata, String project, Boolean overwrite, Boolean ignoreBlank) throws NoServerPrefException, NotFoundException {
        return null;
    }
}
