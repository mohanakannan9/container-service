package org.nrg.actions.daos;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class AceDao extends AbstractHibernateDAO<ActionContextExecution> {
}
