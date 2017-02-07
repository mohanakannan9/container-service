package org.nrg.containers.daos;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CommandDao extends AbstractHibernateDAO<Command> {
    private static final Logger log = LoggerFactory.getLogger(CommandDao.class);

    @Override
    public void initialize(final Command command) {
        if (command == null) {
            return;
        }
        Hibernate.initialize(command);
        Hibernate.initialize(command.getEnvironmentVariables());
        Hibernate.initialize(command.getMounts());
        Hibernate.initialize(command.getInputs());
        Hibernate.initialize(command.getOutputs());
        Hibernate.initialize(command.getXnatCommandWrappers());
        if (command.getXnatCommandWrappers() != null) {
            for (final XnatCommandWrapper xnatCommandWrapper : command.getXnatCommandWrappers()) {
                Hibernate.initialize(xnatCommandWrapper.getExternalInputs());
                Hibernate.initialize(xnatCommandWrapper.getDerivedInputs());
                Hibernate.initialize(xnatCommandWrapper.getOutputHandlers());
            }
        }
    }

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
