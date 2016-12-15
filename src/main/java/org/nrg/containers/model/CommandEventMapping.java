package org.nrg.containers.model;

import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Audited
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"eventType", "commandId"})})
public class CommandEventMapping extends AbstractHibernateEntity {

    private String eventType;
    private Long commandId;
    private String projectId;
    private String groupId;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

}
