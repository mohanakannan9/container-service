package org.nrg.actions.services;

import org.nrg.actions.daos.ScriptEnvironmentDao;
import org.nrg.actions.model.ScriptEnvironment;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.services.DockerImageService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernateScriptEnvironmentService
        extends AbstractHibernateEntityService<ScriptEnvironment, ScriptEnvironmentDao>
        implements ScriptEnvironmentService {
    @Autowired
    private DockerImageService dockerImageService;

//    @Override
//    @Transactional
//    public ScriptEnvironment create(final ScriptEnvironment scriptEnvironment) {
//        if (scriptEnvironment.getDockerImage() != null) {
//            final DockerImage image = dockerImageService.retrieve(scriptEnvironment.getDockerImage().getId());
//            if (image != null) {
//                scriptEnvironment.setDockerImage(image);
//            }
//        }
//        return super.create(scriptEnvironment);
//    }
}
