package org.nrg.execution.services;


import org.nrg.execution.daos.CommandEventMappingDao;
import org.nrg.execution.model.CommandEventMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;

@Service
@Transactional
public class HibernateCommandEventMappingService extends AbstractHibernateEntityService<CommandEventMapping, CommandEventMappingDao>
        implements CommandEventMappingService {

    @Override
    public List<CommandEventMapping> findByEventType(String eventType) {
            return getDao().findByEventType(eventType);
    }
}
