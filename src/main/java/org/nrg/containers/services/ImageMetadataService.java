package org.nrg.containers.services;

import org.nrg.containers.model.metadata.ImageMetadata;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

@SuppressWarnings("unused")
public interface ImageMetadataService extends BaseHibernateService<ImageMetadata> {
    ImageMetadata getByImageId(final String imageId);
}
