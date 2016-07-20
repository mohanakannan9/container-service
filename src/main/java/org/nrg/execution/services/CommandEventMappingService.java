package org.nrg.execution.services;


import org.nrg.execution.model.CommandEventMapping;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;


public interface CommandEventMappingService extends BaseHibernateService<CommandEventMapping> {

    List<CommandEventMapping> findByEventType(String eventType);

}
