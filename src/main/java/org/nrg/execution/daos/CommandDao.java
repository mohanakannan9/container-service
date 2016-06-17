package org.nrg.execution.daos;

import org.nrg.execution.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class CommandDao extends AbstractHibernateDAO<Command> {
}
