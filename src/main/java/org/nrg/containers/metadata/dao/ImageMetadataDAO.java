package org.nrg.containers.metadata.dao;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ImageMetadataDAO extends AbstractHibernateDAO<ImageMetadata> {
    private static final Logger _log = LoggerFactory.getLogger(ImageMetadataDAO.class);

//    @SuppressWarnings("unchecked")
//    public ImageMetadata getByImageId(final String imageId) {
//        if (_log.isDebugEnabled()) {
//            _log.debug("Finding metadata for image ID {}", imageId);
//        }
//        List<ImageMetadata> listQuery = getSession()
//                .createQuery("from org.nrg.containers.metadata.ImageMetadata as meta where :imageId in elements(meta.imageId)")
//                .setString("imageId", imageId).list();
//        if (listQuery.size() > 1) {
//            // This is a problem
//            String message = "Found multiple ImageMetadata rows for image "+imageId+". Ids {";
//            for (ImageMetadata meta : listQuery) {
//                message += meta.getId()+", ";
//            }
//            message = message.substring(0, message.length()-2);
//            message += "}.";
//            _log.error(message);
//            throw new ImageMetadataException(message);
//        } else if (listQuery.size() == 0) {
//            return null;
//        }
//        return listQuery.get(0);
//    }

    /**
     * @param imageId Find metadata for the image with given ID
     * @return List of ImageMetadata for that image
     */
    public List<ImageMetadata> getByImageId(final String imageId) {
        Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("imageId", imageId));
        return criteria.list();
    }

    public List<ImageMetadata> getByType(final String type) {
        Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("type", type));
        return criteria.list();
    }
}
