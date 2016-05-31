package org.nrg.actions.services;

import org.nrg.actions.daos.CommandDao;
import org.nrg.actions.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;

@Service
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
}
