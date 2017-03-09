package org.nrg.containers.daos;

import org.hibernate.Hibernate;
import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.model.ContainerEntityHistory;
import org.nrg.containers.model.ContainerEntityMount;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Repository
public class ContainerEntityRepository extends AbstractHibernateDAO<ContainerEntity> {

    @Override
    public void initialize(final ContainerEntity entity) {
        if (entity == null) {
            return;
        }
        Hibernate.initialize(entity);
        Hibernate.initialize(entity.getEnvironmentVariables());
        Hibernate.initialize(entity.getHistory());
        Hibernate.initialize(entity.getMounts());
        if (entity.getMounts() != null) {
            for (final ContainerEntityMount mount : entity.getMounts()) {
                Hibernate.initialize(mount.getInputFiles());
            }
        }
        Hibernate.initialize(entity.getCommandLine());
        Hibernate.initialize(entity.getRawInputValues());
        Hibernate.initialize(entity.getXnatInputValues());
        Hibernate.initialize(entity.getCommandInputValues());
        Hibernate.initialize(entity.getOutputs());
        Hibernate.initialize(entity.getLogPaths());
    }

    @Nullable
    public ContainerEntity retrieveByContainerId(final @Nonnull String containerId) {
        return findByUniqueProperty("containerId", containerId);
    }

    public boolean eventHasBeenRecorded(final String containerId,
                                        final String status,
                                        final long timestamp) {
        return getSession().createQuery("select 1 from ContainerEntityEvent as event join event.containerEntity as entity " +
                "where event.status = :status and event.externalTimestamp = :time and entity.containerId = :containerId")
                .setLong("time", timestamp)
                .setString("status", status)
                .setString("containerId", containerId)
                .uniqueResult() != null;

    }

    public void persistEvent(final ContainerEntityHistory containerEntityHistory) {
        getSession().persist(containerEntityHistory);
    }
}
