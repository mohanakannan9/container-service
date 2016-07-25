package org.nrg.execution.daos;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.model.Command;
import org.nrg.execution.services.HibernateCommandService;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CommandDao extends AbstractHibernateDAO<Command> {
    private static final Logger log = LoggerFactory.getLogger(CommandDao.class);

    public Command retrieve(final String name, final String dockerImageId) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(dockerImageId)) {
            return null;
        }

        final Map<String, Object> properties = Maps.newHashMap();
        properties.put("name", name);
        properties.put("dockerImage", dockerImageId);

        final List<Command> commands = findByProperties(properties);

        if (commands == null || commands.isEmpty()) {
            return null;
        } else if (commands.size() > 1) {
            if (log.isErrorEnabled()) {
                StringBuilder message = new StringBuilder("Somehow the database contains more than one Command with the same name + docker image id: ");
                for (final Command command : commands) {
                    message.append(command.getId());
                    message.append(", ");
                }
                message.delete(message.lastIndexOf(","), message.length());
                log.error(message.toString());
            }
        }
        return commands.get(0);
    }
}
