package org.nrg.automation.model;

import com.google.common.base.MoreObjects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.actions.model.Command;
import org.nrg.automation.entities.Script;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Objects;

@Audited
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@DiscriminatorValue(ScriptCommand.COMMAND_TYPE)
public class ScriptCommand extends Command {
    static final String COMMAND_TYPE = "script";
    private Script script;

    @ManyToOne
    public Script getScript() {
        return script;
    }

    public void setScript(final Script script) {
        this.script = script;
    }

    @Override
    public void run() {
        // TODO
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || !super.equals(o) || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final ScriptCommand that = (ScriptCommand) o;
        return Objects.equals(script, that.script);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), script);
    }

    @Override
    public String toString() {
        return addParentFields(MoreObjects.toStringHelper(this))
                .add("script", script)
                .toString();
    }

}
