package org.nrg.containers.services.impl;

import org.hibernate.Hibernate;
import org.nrg.containers.daos.XnatCommandWrapperRepository;
import org.nrg.containers.model.XnatCommandInput;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.services.XnatCommandWrapperService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class HibernateXnatCommandWrapperService
        extends AbstractHibernateEntityService<XnatCommandWrapper, XnatCommandWrapperRepository>
        implements XnatCommandWrapperService {

    @Override
    public void initialize(final XnatCommandWrapper xnatCommandWrapper) {
        if (xnatCommandWrapper == null) {
            return;
        }
        Hibernate.initialize(xnatCommandWrapper);
        Hibernate.initialize(xnatCommandWrapper.getInputs());

        initializeInputList(xnatCommandWrapper.getDerivedInputs());
        initializeInputList(xnatCommandWrapper.getInputs());
    }

    private void initializeInputList(final Set<XnatCommandInput> inputs) {
        if (inputs != null) {
            Hibernate.initialize(inputs);

            for (final XnatCommandInput input : inputs) {
                Hibernate.initialize(input.getCommandOutputHandlers());
            }
        }
    }
}
