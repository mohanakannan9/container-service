package org.nrg.containers.daos;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.CommandEventMapping;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class CommandEventMappingDao extends AbstractHibernateDAO<CommandEventMapping> {

    /**
     * Find CommandIds that are configured to run for a given eventType.
     * If eventType is null, return an emply list.
     *
     * @param eventType Find Commands that are triggered by this eventType.
     * @return List of Commands.
     */
    public List<CommandEventMapping> findByEventType(final String eventType) {
        if (eventType == null || StringUtils.isBlank(eventType)) {
            return Lists.newArrayList();
        }
        return findByProperty("eventType", eventType);
    }
}
