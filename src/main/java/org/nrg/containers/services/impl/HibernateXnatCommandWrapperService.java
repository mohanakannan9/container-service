package org.nrg.containers.services.impl;

import org.nrg.containers.daos.XnatCommandWrapperRepository;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.services.XnatCommandWrapperService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;

@Service
public class HibernateXnatCommandWrapperService
        extends AbstractHibernateEntityService<XnatCommandWrapper, XnatCommandWrapperRepository>
        implements XnatCommandWrapperService {
}
