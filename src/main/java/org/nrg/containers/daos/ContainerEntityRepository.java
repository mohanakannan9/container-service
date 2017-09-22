package org.nrg.containers.daos;

import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.container.entity.ContainerEntityMount;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Repository
public class ContainerEntityRepository extends AbstractHibernateDAO<ContainerEntity> {
    private static final Logger log = LoggerFactory.getLogger(ContainerEntityRepository.class);

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
        Hibernate.initialize(entity.getInputs());
        Hibernate.initialize(entity.getOutputs());
        Hibernate.initialize(entity.getLogPaths());
    }

    @Nullable
    public ContainerEntity retrieveByContainerId(final @Nonnull String containerId) {
        return findByUniqueProperty("containerId", containerId);
    }

    public void addHistoryItem(final @Nonnull ContainerEntity containerEntity,
                               final @Nonnull ContainerEntityHistory containerEntityHistory) {
        containerEntity.addToHistory(containerEntityHistory);
        getSession().persist(containerEntityHistory);
        if (containerEntityHistory.getTimeRecorded() != null && (containerEntity.getStatusTime() == null ||
                containerEntityHistory.getTimeRecorded().getTime() > containerEntity.getStatusTime().getTime())) {
            containerEntity.setStatusTime(containerEntityHistory.getTimeRecorded());
            containerEntity.setStatus(containerEntityHistory.getStatus());
            log.debug("Setting container entity {} status to \"{}\", based on history entry status \"{}\".",
                    containerEntity.getId(),
                    containerEntity.getStatus(),
                    containerEntityHistory.getStatus());
        }
        update(containerEntity);
    }

    @Nonnull
    public List<ContainerEntity> retrieveServices() {
        final List servicesResult = getSession()
                .createCriteria(ContainerEntity.class)
                .add(Restrictions.isNotNull("serviceId"))
                .list();
        return initializeAndReturnServiceList(servicesResult);
    }

    @Nonnull
    public List<ContainerEntity> retrieveNonfinalizedServices() {
        final List servicesResult = getSession()
                .createCriteria(ContainerEntity.class)
                .add(Restrictions.conjunction()
                        .add(Restrictions.isNotNull("serviceId"))
                        .add(Restrictions.not(Restrictions.disjunction()
                                .add(Restrictions.like("status", "Complete"))
                                .add(Restrictions.like("status", "Done"))
                                .add(Restrictions.like("status", "Failed"))
                                .add(Restrictions.like("status", "Killed"))
                        ))
                )
                .list();
        return initializeAndReturnServiceList(servicesResult);
    }

    @SuppressWarnings("unchecked")
    private List<ContainerEntity> initializeAndReturnServiceList(final List servicesResult) {
        if (servicesResult != null) {
            try {
                final List<ContainerEntity> toReturn = (List<ContainerEntity>) servicesResult;
                for (final ContainerEntity containerEntity : toReturn) {
                    initialize(containerEntity);
                }
                return toReturn;
            } catch (ClassCastException e) {
                log.error("Failed to cast service search results to ContainerEntity.", e);
            }
        }
        return Collections.emptyList();
    }
}
