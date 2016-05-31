package org.nrg.actions.services;

import org.nrg.actions.daos.ActionDao;
import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionDto;
import org.nrg.actions.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HibernateActionService extends AbstractHibernateEntityService<Action, ActionDao>
        implements ActionService {
    @Autowired
    private CommandService commandService;

    private Action newFromDto(final ActionDto actionDto) {
        final Command command = commandService.retrieve(actionDto.getCommandId());
        return new Action(actionDto, command);
    }

    @Override
    public Action createFromDto(final ActionDto actionDto) {
        final Action action = newFromDto(actionDto);
        create(action);
        return action;
    }

    @Override
    public List<Action> findByRootXsiType(final String xsiType) {
        // TODO What is the property for root.xsiType?
        return getDao().findByRootXsiType(xsiType);
    }

    @Override
    public void updateFromDto(final ActionDto actionDto) {
        update(newFromDto(actionDto));
    }

    @Override
    public void afterPropertiesSet() {
        setInitialize(true);
    }
}
