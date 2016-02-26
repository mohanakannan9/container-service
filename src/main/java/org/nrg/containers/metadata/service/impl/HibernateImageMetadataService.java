package org.nrg.containers.metadata.service.impl;

import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.metadata.dao.ImageMetadataDAO;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernateImageMetadataService extends AbstractHibernateEntityService<ImageMetadata, ImageMetadataDAO> implements ImageMetadataService {
    @Override
    @Transactional
    public ImageMetadata getByImageId(String imageId) {
        return getDao().getByImageId(imageId);
    }
}
