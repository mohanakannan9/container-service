package org.nrg.actions.model.tree;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.actions.model.CommandInput;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class RuntimeTree extends AbstractHibernateEntity {
    private CommandInput input;
    private RuntimeTreeNode root;
    // RuntimeTreeNode will need to be stored as JSON. It may be tricky to get Hibernate to
    // map that JSON as the correct object type.
    // See http://stackoverflow.com/questions/15974474/mapping-postgresql-json-column-to-hibernate-value-type
}
