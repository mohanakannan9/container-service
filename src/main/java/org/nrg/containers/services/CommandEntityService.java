package org.nrg.containers.services;

import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;
import java.util.Map;

public interface CommandEntityService extends BaseHibernateService<CommandEntity> {
    List<CommandEntity> findByProperties(Map<String, Object> properties);
    CommandWrapperEntity addWrapper(CommandEntity commandEntity, CommandWrapperEntity wrapperToAdd);
}
