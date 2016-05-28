package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.actions.model.tree.MatchTreeNode;
import org.nrg.actions.model.tree.MatchTreeNodeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionInput implements Serializable {

    @JsonProperty("name") private String inputName;
    @JsonProperty("command-variable-name") private String commandVariableName;
    @JsonProperty("match-tree") private MatchTreeNode root;
    private Boolean required;

    public ActionInput() {}

    public ActionInput(final CommandVariable commandVariableName) {
        this.inputName = commandVariableName.getName();
        this.commandVariableName = commandVariableName.getName();
        this.required = commandVariableName.getRequired();
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getCommandVariableName() {
        return commandVariableName;
    }

    public void setCommandVariableName(final String commandVariableName) {
        if (this.inputName == null) {
            this.inputName = commandVariableName;
        }
        this.commandVariableName = commandVariableName;
    }

    public Boolean getRequired() {
        return required;
    }

    public Boolean isRequired() {
        return required;
    }

    public void setRequired(final Boolean required) {
        this.required = required;
    }

    @Column(columnDefinition = "varchar")
    @Convert(converter = MatchTreeNodeConverter.class)
    public MatchTreeNode getRoot() {
        return root;
    }

    public void setRoot(final MatchTreeNode root) {
        this.root = root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActionInput that = (ActionInput) o;
        return Objects.equals(this.inputName, that.inputName) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.commandVariableName, that.commandVariableName) &&
                Objects.equals(this.root, that.root);

    }

    @Override
    public int hashCode() {
        return Objects.hash(inputName, required, commandVariableName, root);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", inputName)
                .add("required", required)
                .add("commandVariable", commandVariableName)
                .add("root", root)
                .toString();
    }

}
