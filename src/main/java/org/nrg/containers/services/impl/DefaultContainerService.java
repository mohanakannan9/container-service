package org.nrg.containers.services.impl;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.exceptions.ContainerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.metadata.ImageMetadataAnn;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.containers.model.Container;
import org.nrg.containers.model.ContainerServer;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.utilities.Reflection;
import org.nrg.prefs.entities.Preference;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.PreferenceService;
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

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection") // IntelliJ does not process the excludeFilter in ContainerServiceConfig @ComponentScan, erroneously marks this red
    private PreferenceService prefsService;

    public ContainerServer getServer() throws NoServerPrefException {
        final Preference serverPref = prefsService.getPreference(SERVER_PREF_TOOL_ID, SERVER_PREF_NAME);
        if (serverPref == null || StringUtils.isBlank(serverPref.getValue())) {
            throw new NoServerPrefException("No container server URI defined in preferences.");
        }
        return new ContainerServer(serverPref.getValue());
    }

    @Override
    public void setServer(final String host) throws InvalidPreferenceName {
        prefsService.setPreference(SERVER_PREF_TOOL_ID, SERVER_PREF_NAME, host);
    }

    private String server() throws NoServerPrefException {
        return getServer().host();
    }

    public List<Image> getAllImages() throws NoServerPrefException {
        return controlApi.getAllImages(server());
    }

    public Image getImageByName(final String name) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getImageByName(server(), name);
    }

    @Override
    public Image getImageById(String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        // TODO Figure out what to do with this. Not sure if we need to be fetching images by id.
        return controlApi.getImageById(server(), id);
    }

    @Override
    public String deleteImageById(final String id, final Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return null;
    }

    @Override
    public String deleteImageByName(final String name, final Boolean onServer) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return null;
    }

    public List<Container> getAllContainers() throws NoServerPrefException, ContainerServerException {
        return controlApi.getAllContainers(server());
    }

    public String getContainerStatus(final String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getContainerStatus(server(), id);
    }

    public Container getContainer(final String id) throws NoServerPrefException, NotFoundException, ContainerServerException {
        return controlApi.getContainer(server(), id);
    }

    @Override
    public String launch(String imageName, ImageParameters params) throws NoServerPrefException, NotFoundException, ContainerServerException {
//        final Image image = controlApi.getImageByName(server, imageName);
//        final ImageMetadata imageMetadata = _imageMetadataService.getByImageId(image.id());
        return controlApi.launchImage(server(), imageName, params.getCommandArray(), params.getVolumesArray());
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
