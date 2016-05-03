package org.nrg.containers.image.service;

import org.nrg.containers.image.dao.ImageDAO;
import org.nrg.containers.image.Image;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HibernateImageService
    extends AbstractHibernateEntityService<Image, ImageDAO>
    implements ImageService {

    private static final Logger _log = LoggerFactory.getLogger(HibernateImageService.class);

    @Override
    @Transactional
    public List<Image> getByImageId(final String imageId) {
        return getDao().getByImageId(imageId);
    }


}
