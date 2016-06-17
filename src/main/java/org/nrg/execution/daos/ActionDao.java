package org.nrg.execution.daos;

import org.hibernate.criterion.Restrictions;
import org.nrg.execution.model.Action;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ActionDao extends AbstractHibernateDAO<Action> {

    /**
     * Find Actions with that are configured to run for a given xsiType.
     * If xsiType is null, that means Actions are site-wide.
     *
     * @param xsiType Find Actions that can run on this. Can be null for site-wide Actions.
     * @return List of Actions that can run on given xsiType.
     */
    public List<Action> findByRootXsiType(final String xsiType) {
        final List list =
                findByCriteria(Restrictions.like("rootXsiType", xsiType),
                        Restrictions.eq("enabled", true));
        if (list == null || list.size() == 0) {
            return null;
        } else {
            return (List<Action>)list;
        }
    }
}
