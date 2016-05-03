package org.nrg.containers.metadata.service;

import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.model.DockerImage;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

@SuppressWarnings("unused")
public interface ImageMetadataService extends BaseHibernateService<ImageMetadata> {

    List<ImageMetadata> getByImageId(String imageId);

    void setMetadata(DockerImage image, ImageMetadata metadata, String project, Boolean overwrite, Boolean ignoreBlank);

    ImageMetadata getMetadataFromContext(String context);
}
