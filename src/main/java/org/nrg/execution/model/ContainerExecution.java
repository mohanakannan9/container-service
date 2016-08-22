package org.nrg.execution.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.List;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"containerId"})})
public class ContainerExecution extends AbstractHibernateEntity {
    private String containerId;
    private String userId;
    private String rootObjectId;
    private String rootObjectXsiType;
    private List<ContainerExecutionHistory> history = Lists.newArrayList();

    public ContainerExecution() {}

    public ContainerExecution(final ResolvedCommand resolvedCommand, final String containerId, final String userId) {
        this.containerId = containerId;
        this.userId = userId;

        // TODO the resolved command doesn't have this info. where can I get it?
//        this.rootObjectId = resolvedCommand.
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(final String user) {
        this.userId = user;
    }

    public String getRootObjectId() {
        return rootObjectId;
    }

    public void setRootObjectId(final String rootObjectId) {
        this.rootObjectId = rootObjectId;
    }

    public String getRootObjectXsiType() {
        return rootObjectXsiType;
    }

    public void setRootObjectXsiType(final String rootObjectXsiType) {
        this.rootObjectXsiType = rootObjectXsiType;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    public List<ContainerExecutionHistory> getHistory() {
        return history;
    }

    public void setHistory(final List<ContainerExecutionHistory> history) {
        this.history = history;
    }

    @Transient
    public void addToHistory(final ContainerExecutionHistory historyItem) {
        if (this.history == null) {
            this.history = Lists.newArrayList();
        }
        this.history.add(historyItem);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("containerId", containerId)
                .add("userId", userId)
                .add("rootObjectId", rootObjectId)
                .add("rootObjectXsiType", rootObjectXsiType)
                .add("history", history)
                .toString();
    }
}
