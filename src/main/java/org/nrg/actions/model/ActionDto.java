package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

public class ActionDto {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("command-id") private Long commandId;
    private List<ActionInput> inputs;
    private List<ActionOutput> outputs;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getCommandId() {
        return commandId;
    }

    public void setCommandId(final Long commandId) {
        this.commandId = commandId;
    }

    public List<ActionInput> getInputs() {
        return inputs;
    }

    public void setInputs(final List<ActionInput> inputs) {
        this.inputs = inputs;
    }

    public List<ActionOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ActionOutput> outputs) {
        this.outputs = outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionDto actionDto = (ActionDto) o;
        return Objects.equals(this.id, actionDto.id) &&
                Objects.equals(this.name, actionDto.name) &&
                Objects.equals(this.description, actionDto.description) &&
                Objects.equals(this.commandId, actionDto.commandId) &&
                Objects.equals(this.inputs, actionDto.inputs) &&
                Objects.equals(this.outputs, actionDto.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, commandId, inputs, outputs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .add("commandId", commandId)
                .add("inputs", inputs)
                .add("outputs", outputs)
                .toString();
    }

}
