package org.nrg.containers.services;


import org.nrg.containers.daos.CommandEventMappingDao;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
