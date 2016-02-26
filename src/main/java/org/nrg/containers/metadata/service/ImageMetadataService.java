package org.nrg.containers.metadata.service;

import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

@SuppressWarnings("unused")
public interface ImageMetadataService extends BaseHibernateService<ImageMetadata> {
    ImageMetadata getByImageId(final String imageId);
}
