package org.nrg.execution.daos;

import org.nrg.execution.model.ActionContextExecution;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class AceDao extends AbstractHibernateDAO<ActionContextExecution> {
}
