package org.nrg.containers.services;

import org.nrg.containers.model.command.entity.CommandEntity;
import org.nrg.containers.model.command.entity.CommandWrapperEntity;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;
import java.util.Map;

public interface CommandEntityService extends BaseHibernateService<CommandEntity> {
    List<CommandEntity> findByProperties(Map<String, Object> properties);

    CommandWrapperEntity addWrapper(CommandEntity commandEntity, CommandWrapperEntity wrapperToAdd);
    CommandWrapperEntity retrieve(long commandId, long wrapperId);
    CommandWrapperEntity retrieve(final CommandEntity commandEntity, long wrapperId);
    CommandWrapperEntity get(long commandId, long wrapperId) throws NotFoundException;
    CommandWrapperEntity get(final CommandEntity commandEntity, long wrapperId) throws NotFoundException;
    CommandWrapperEntity update(CommandWrapperEntity updates) throws NotFoundException;
    void delete(long commandId, long wrapperId);

    void assertPairExists(long commandId, String wrapperName) throws NotFoundException;
}
