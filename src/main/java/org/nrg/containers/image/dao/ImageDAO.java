package org.nrg.containers.image.dao;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.containers.image.Image;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ImageDAO extends AbstractHibernateDAO<Image> {
    private static final Logger _log = LoggerFactory.getLogger(ImageDAO.class);

    /**
     * @param imageId Find metadata for the image with given ID
     * @return List of ImageMetadata for that image
     */
    public List<Image> getByImageId(final String imageId) {
        Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("imageId", imageId));
        return criteria.list();
    }

}
