package org.nrg.actions.services;

import org.nrg.actions.daos.ActionDao;
import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionDto;
import org.nrg.actions.model.Command;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateActionService extends AbstractHibernateEntityService<Action, ActionDao>
        implements ActionService {
    @Autowired
    private CommandService commandService;

    @Override
    public Action createFromDto(final ActionDto actionDto) {
        final Command command = commandService.retrieve(actionDto.getCommandId());
        final Action action = new Action(actionDto, command);
        create(action);
        return action;
    }
}
