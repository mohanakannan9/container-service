package org.nrg.actions.services;

import org.nrg.actions.model.ScriptEnvironment;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public interface ScriptEnvironmentService extends BaseHibernateService<ScriptEnvironment> {
}
