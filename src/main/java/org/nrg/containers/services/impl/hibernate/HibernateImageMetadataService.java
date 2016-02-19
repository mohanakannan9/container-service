package org.nrg.containers.services.impl.hibernate;

import org.nrg.containers.model.metadata.ImageMetadata;
import org.nrg.containers.repositories.ImageMetadataRepository;
import org.nrg.containers.services.ImageMetadataService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unused")
@Service
public class HibernateImageMetadataService extends AbstractHibernateEntityService<ImageMetadata, ImageMetadataRepository> implements ImageMetadataService {
    @Override
    @Transactional
    public ImageMetadata getByImageId(String imageId) {
        return getDao().getByImageId(imageId);
    }
}
