package org.nrg.containers.services;

import org.nrg.containers.model.Container;
import org.nrg.containers.model.Image;
import org.nrg.containers.model.ImageParameters;

import java.util.List;

public interface ContainerService {
    List<Image> getAllImages();

    Image getImageByName(final String name);

    List<Container> getAllContainers();

    String getContainerStatus(final String id);

    Container getContainer(final String id);

    String launch(final String imageName, final ImageParameters params);
}
