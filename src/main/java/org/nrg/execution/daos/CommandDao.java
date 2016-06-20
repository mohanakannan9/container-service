package org.nrg.execution.daos;

import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommandDao extends AbstractHibernateDAO<Command> {
    public List<Command> findByName(final String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        return findByProperty("name", name);
    }
}
