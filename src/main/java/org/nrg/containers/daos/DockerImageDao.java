package org.nrg.containers.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.containers.model.DockerImage;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DockerImageDao extends AbstractHibernateDAO<DockerImage> {
    /**
     * @param imageId Find metadata for the image with given ID
     * @return List of ImageMetadata for that image
     */
    public List<DockerImage> getByImageId(final String imageId) {
        Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("imageId", imageId));
        return criteria.list();
    }
}
