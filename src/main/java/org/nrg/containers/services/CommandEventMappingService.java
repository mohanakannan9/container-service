package org.nrg.containers.services;


import org.nrg.containers.model.CommandEventMapping;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;


public interface CommandEventMappingService extends BaseHibernateService<CommandEventMapping> {

    List<CommandEventMapping> findByEventType(String eventType);

}
