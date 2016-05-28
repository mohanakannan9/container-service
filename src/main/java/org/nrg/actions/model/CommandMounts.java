package org.nrg.actions.model;

import com.google.common.base.MoreObjects;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;
import java.util.Objects;

@Embeddable
public class CommandMounts {

    private List<Mount> inputs;
    private List<Mount> outputs;

    @ElementCollection
    public List<Mount> getInputs() {
        return inputs;
    }

    public void setInputs(final List<Mount> inputs) {
        this.inputs = inputs;
    }

    @ElementCollection
    public List<Mount> getOutputs() {
        return outputs;
    }

    public void setOutputs(final List<Mount> outputs) {
        this.outputs = outputs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandMounts that = (CommandMounts) o;
        return Objects.equals(this.inputs, that.inputs) &&
                Objects.equals(this.outputs, that.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, outputs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .toString();
    }
}
