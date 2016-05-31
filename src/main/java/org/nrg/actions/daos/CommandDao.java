package org.nrg.actions.daos;

import org.nrg.actions.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class CommandDao extends AbstractHibernateDAO<Command> {
}
