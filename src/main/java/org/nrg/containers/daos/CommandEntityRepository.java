package org.nrg.containers.daos;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.NonUniqueObjectException;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Repository
public class CommandEntityRepository extends AbstractHibernateDAO<CommandEntity> {
    private static final Logger log = LoggerFactory.getLogger(CommandEntityRepository.class);

    @Override
    public void initialize(final CommandEntity commandEntity) {
        if (commandEntity == null) {
            return;
        }
        Hibernate.initialize(commandEntity);
        Hibernate.initialize(commandEntity.getEnvironmentVariables());
        Hibernate.initialize(commandEntity.getMounts());
        Hibernate.initialize(commandEntity.getInputs());
        Hibernate.initialize(commandEntity.getOutputs());
        Hibernate.initialize(commandEntity.getCommandWrapperEntities());
        if (commandEntity.getCommandWrapperEntities() != null) {
            for (final CommandWrapperEntity commandWrapperEntity : commandEntity.getCommandWrapperEntities()) {
                Hibernate.initialize(commandWrapperEntity.getExternalInputs());
                Hibernate.initialize(commandWrapperEntity.getDerivedInputs());
                Hibernate.initialize(commandWrapperEntity.getOutputHandlers());
            }
        }
    }

    @Nullable
    public CommandEntity retrieve(final String name, final String dockerImageId) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(dockerImageId)) {
            return null;
        }

        final Map<String, Object> properties = Maps.newHashMap();
        properties.put("name", name);
        properties.put("dockerImage", dockerImageId);

        final List<CommandEntity> commandEntities = findByProperties(properties);

        if (commandEntities == null || commandEntities.isEmpty()) {
            return null;
        } else if (commandEntities.size() > 1) {
            if (log.isErrorEnabled()) {
                StringBuilder message = new StringBuilder("Somehow the database contains more than one Command with the same name + docker image id: ");
                for (final CommandEntity commandEntity : commandEntities) {
                    message.append(commandEntity.getId());
                    message.append(", ");
                }
                message.delete(message.lastIndexOf(","), message.length());
                log.error(message.toString());
            }
        }
        final CommandEntity commandEntity = commandEntities.get(0);
        initialize(commandEntity);
        return commandEntities.get(0);
    }

    @Override
    @Nullable
    public List<CommandEntity> findByProperties(@Nonnull final Map<String, Object> properties) {
        final List<CommandEntity> commandEntityList = super.findByProperties(properties);
        if (commandEntityList == null) {
            return null;
        }
        for (final CommandEntity commandEntity : commandEntityList) {
            initialize(commandEntity);
        }
        return commandEntityList;
    }

    @Override
    public void update(@Nonnull final CommandEntity commandEntity) {
        try {
            getSession().update(commandEntity);
        } catch (NonUniqueObjectException e) {
            getSession().merge(commandEntity);
        }
    }
}
