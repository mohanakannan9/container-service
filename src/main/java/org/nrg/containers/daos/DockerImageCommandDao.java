package org.nrg.containers.daos;

import org.hibernate.Session;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DockerImageCommandDao extends AbstractHibernateDAO<DockerImageCommand> {
    private static final Logger _log = LoggerFactory.getLogger(DockerImageCommandDao.class);


//    /**
//     * @param imageId Find metadata for the image with given ID
//     * @return List of ImageMetadata for that image
//     */
//    public List<ImageMetadata> getByImageId(final String imageId) {
//        Criteria criteria = getSession().createCriteria(getParameterizedType());
//        criteria.add(Restrictions.eq("imageId", imageId));
//        return criteria.list();
//    }
//
//    public List<ImageMetadata> getByType(final String type) {
//        Criteria criteria = getSession().createCriteria(getParameterizedType());
//        criteria.add(Restrictions.eq("type", type));
//        return criteria.list();
//    }
}
