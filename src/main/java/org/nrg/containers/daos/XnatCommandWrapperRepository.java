package org.nrg.containers.daos;

import org.hibernate.Hibernate;
import org.nrg.containers.model.XnatCommandInput;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class XnatCommandWrapperRepository extends AbstractHibernateDAO<XnatCommandWrapper> {
    @Override
    public void initialize(final XnatCommandWrapper xnatCommandWrapper) {
        if (xnatCommandWrapper == null) {
            return;
        }
        Hibernate.initialize(xnatCommandWrapper);
        Hibernate.initialize(xnatCommandWrapper.getOutputHandlers());

        initializeInputList(xnatCommandWrapper.getExternalInputs());
        initializeInputList(xnatCommandWrapper.getDerivedInputs());
    }

    private void initializeInputList(final Set<XnatCommandInput> inputs) {
        if (inputs != null) {
            Hibernate.initialize(inputs);

            for (final XnatCommandInput input : inputs) {
                Hibernate.initialize(input.getProvidesValueForCommandInput());
            }
        }
    }
}
