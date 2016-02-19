package org.nrg.containers.repositories;

import org.nrg.containers.exceptions.ImageMetadataException;
import org.nrg.containers.model.metadata.ImageMetadata;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ImageMetadataRepository extends AbstractHibernateDAO<ImageMetadata> {
    private static final Logger _log = LoggerFactory.getLogger(ImageMetadataRepository.class);

    @SuppressWarnings("unchecked")
    public ImageMetadata getByImageId(final String imageId) {
        if (_log.isDebugEnabled()) {
            _log.debug("Finding metadata for image ID {}", imageId);
        }
        List<ImageMetadata> listQuery = getSession()
                .createQuery("from org.nrg.containers.model.metadata.ImageMetadata as meta where :imageId in elements(meta.imageId)")
                .setString("imageId", imageId).list();
        if (listQuery.size() > 1) {
            // This is a problem
            String message = "Found multiple ImageMetadata rows for image "+imageId+". Ids {";
            for (ImageMetadata meta : listQuery) {
                message += meta.getId()+", ";
            }
            message = message.substring(0, message.length()-2);
            message += "}.";
            _log.error(message);
            throw new ImageMetadataException(message);
        } else if (listQuery.size() == 0) {
            return null;
        }
        return listQuery.get(0);
    }
}
