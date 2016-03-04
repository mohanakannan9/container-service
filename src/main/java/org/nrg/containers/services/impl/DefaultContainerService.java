package org.nrg.containers.services.impl;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoHubException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.metadata.ImageMetadataAnn;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerHub;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.utilities.Reflection;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
        return null;
    }

    @Override
    public String deleteImageByName(final String name, final Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException {
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
    public String getContainerLogs(final String id)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        return null;
    }

    @Override
    public String verbContainer(final String id, final String status)
            throws NoServerPrefException, NotFoundException, ContainerServerException {
        return null;
    }

    @Override
    public ContainerHub getHub(final String hub, final Boolean verbose) throws NotFoundException {
        return null;
    }

    @Override
    public List<ContainerHub> getHubs(final Boolean verbose) throws NotFoundException {
        return null;
    }

    @Override
    public void setHub(final ContainerHub hub, final Boolean overwrite, final Boolean ignoreBlank) {
        return;
    }

    @Override
    public String search(final String term) throws NoHubException {
        return null;
    }

    @Override
    public Image pullByName(final String image, final String hub, final String name)
            throws NoHubException, NotFoundException, ContainerServerException {
        return null;
    }

    @Override
    public Image pullFromSource(final String source, final String name)
            throws NoHubException, NotFoundException, ContainerServerException {
        return null;
    }

    @Override
    public String setMetadataByName(final String name, final Map<String, String> metadata, final String project, final Boolean overwrite, final Boolean ignoreBlank)
            throws NoServerPrefException, NotFoundException {
        // Check if metadata format is valid
        // Get image
        // write metadata to db with image id
        return null;
    }

    @Override
    public String setMetadataById(final String id, final Map<String, String> metadata, final String project, final Boolean overwrite, final Boolean ignoreBlank)
            throws NoServerPrefException, NotFoundException {
        // Check if metadata format is valid
        // Get image
        // write metadata to db with image id
        return null;
    }

    @Override
    public ContainerServer getServer() throws NoServerPrefException, NotFoundException {
        return controlApi.getServer();
    }

    @Override
    public void setServer(String host) throws InvalidPreferenceName {

    }

    static Map<String, Class<? extends ImageMetadata>> imageMetadataClasses = null;

    public static void loadMetadataClasses() {
        if (imageMetadataClasses == null) {
            imageMetadataClasses = Maps.newConcurrentMap();

            try {
                List<Class<?>> classes = Reflection.getClassesForPackage("org.nrg.containers.model.metadata");

                for (Class<?> clazz : classes) {
                    if (ImageMetadata.class.isAssignableFrom(clazz)) {
                        Class<? extends ImageMetadata> metadataClazz = clazz.asSubclass(ImageMetadata.class);
                        if (metadataClazz.isAnnotationPresent(ImageMetadataAnn.class)) {
                            ImageMetadataAnn annotation = metadataClazz.getAnnotation(ImageMetadataAnn.class);
                            if (StringUtils.isBlank(annotation.version())) {
                                String message = "Class " + metadataClazz.getName() + " inherits from ImageMetadata, but has no metadata version set.";
                                _log.error(message);
                            } else {
                                imageMetadataClasses.put(annotation.version(), metadataClazz);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                _log.error("", e);
            }
        }
    }

    /**
     * This method walks the <b>org.nrg.container.model.metadata</b> package and attempts to find versions of the Image Metadata.
     */
    public static ImageMetadata parseMetadata(final Image image, final Map<String, String> metadata) {
        loadMetadataClasses();

        ImageMetadata metadataObj = null;
        if (metadata.containsKey("version")) {
            final String metaVersion = metadata.get("version");
            if (imageMetadataClasses.containsKey(metaVersion)) {
                try {
                    metadataObj = imageMetadataClasses.get(metaVersion).newInstance();
                    metadataObj.setImageId(image.getId());
                    metadataObj.parse(metadata);
                } catch (InstantiationException | IllegalAccessException e) {
                    String message = "Error instantiating image metadata class " + imageMetadataClasses.get(metaVersion).getName() + " for metadata version " + metaVersion;
                    _log.error(message, e);
                }
            } else {
                _log.error("No image metadata class found for metadata version " + metaVersion + ".");
            }
        } else {
            _log.error("Image metadata does not contain a 'version'.");
        }

        return metadataObj;
    }
}
