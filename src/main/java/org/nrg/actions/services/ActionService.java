package org.nrg.actions.services;

import org.nrg.actions.model.Action;
import org.nrg.actions.model.ActionDto;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface ActionService extends BaseHibernateService<Action> {
    Action createFromDto(ActionDto actionDto);
}
