package org.nrg.containers.services.impl;

import com.google.common.collect.Lists;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.containers.daos.CommandEntityRepository;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HibernateCommandEntityService extends AbstractHibernateEntityService<CommandEntity, CommandEntityRepository>
        implements CommandEntityService {
    private static final Logger log = LoggerFactory.getLogger(HibernateCommandEntityService.class);

    @Override
    @Nonnull
    public List<CommandEntity> getAll() {
        final List<CommandEntity> commandEntities = super.getAll();
        return commandEntities == null ? Lists.<CommandEntity>newArrayList() : commandEntities;
    }

    @Override
    @Nonnull
    public CommandEntity create(@Nonnull final CommandEntity commandEntity) throws NrgRuntimeException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Saving command " + commandEntity.getName());
            }
            if (commandEntity.getCommandWrapperEntities() != null) {
                for (final CommandWrapperEntity commandWrapperEntity : commandEntity.getCommandWrapperEntities()) {
                    commandWrapperEntity.setCommandEntity(commandEntity);
                }
            }
            return super.create(commandEntity);
        } catch (ConstraintViolationException e) {
            throw new NrgServiceRuntimeException("This command duplicates a command already in the database.", e);
        }
    }

    @Override
    @Nonnull
    public List<CommandEntity> findByProperties(@Nonnull final Map<String, Object> properties) {
        final List<CommandEntity> commandEntities = getDao().findByProperties(properties);
        return commandEntities == null ? Lists.<CommandEntity>newArrayList() : commandEntities;
    }

    @Override
    public CommandWrapperEntity addWrapper(final @Nonnull CommandEntity commandEntity, final @Nonnull CommandWrapperEntity wrapperToAdd) {
        getDao().addWrapper(commandEntity, wrapperToAdd);
        return wrapperToAdd;
    }
}
