package org.nrg.containers.services;

import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;
import java.util.Map;

public interface CommandEntityService extends BaseHibernateService<CommandEntity> {
    List<CommandEntity> findByProperties(Map<String, Object> properties);

    CommandWrapperEntity addWrapper(CommandEntity commandEntity, CommandWrapperEntity wrapperToAdd);
    CommandWrapperEntity retrieveWrapper(long wrapperId);
    CommandWrapperEntity retrieveWrapper(long commandId, String wrapperName);
    CommandWrapperEntity getWrapper(long wrapperId) throws NotFoundException;
    CommandWrapperEntity getWrapper(long commandId, String wrapperName) throws NotFoundException;
    CommandWrapperEntity update(CommandWrapperEntity updates) throws NotFoundException;
    void deleteWrapper(long wrapperId);

    long getWrapperId(long commandId, String wrapperName) throws NotFoundException;

    CommandEntity getCommandByWrapperId(long wrapperId) throws NotFoundException;

    void throwExceptionIfCommandExists(CommandEntity commandEntity) throws NrgRuntimeException;
}
