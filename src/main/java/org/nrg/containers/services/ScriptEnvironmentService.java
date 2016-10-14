package org.nrg.containers.services;

import org.nrg.containers.model.ScriptEnvironment;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public interface ScriptEnvironmentService extends BaseHibernateService<ScriptEnvironment> {
}
