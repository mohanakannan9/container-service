package org.nrg.containers.metadata.service.impl;

import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.metadata.dao.ImageMetadataDAO;
import org.nrg.containers.metadata.service.ImageMetadataService;
import org.nrg.containers.model.DockerImage;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HibernateImageMetadataService
    extends AbstractHibernateEntityService<ImageMetadata, ImageMetadataDAO>
    implements ImageMetadataService {

    private static final Logger _log = LoggerFactory.getLogger(HibernateImageMetadataService.class);

    @Override
    @Transactional
    public List<ImageMetadata> getByImageId(final String imageId) {
        return getDao().getByImageId(imageId);
    }

    @Override
    public void setMetadata(DockerImage image, ImageMetadata metadata, String project, Boolean overwrite, Boolean ignoreBlank) {
        // TODO
    }

    @Override
    public ImageMetadata getMetadataFromContext(String context){
        return getDao().getByType(context).get(0);
    }


}
