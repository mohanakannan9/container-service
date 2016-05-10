package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.actions.model.tree.MatchTreeNode;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

public class ActionInput implements Serializable {

    @JsonProperty("command-input-name") private String commandInputName;
    @JsonProperty("root-context-property-name") private String rootContextPropertyName;
    @JsonIgnore private String rootContextProperty;
    private MatchTreeNode root;
    private Boolean required;
    private Action parent;

    public String getRootContextPropertyName() {
        return rootContextPropertyName;
    }

    public void setRootContextPropertyName(final String rootContextPropertyName) {
        this.rootContextPropertyName = rootContextPropertyName;
    }

    @Transient
    public String getRootContextProperty() {
        return rootContextProperty;
    }

    public void setRootContextProperty(final String rootContextProperty) {
        this.rootContextProperty = rootContextProperty;
    }

    public String getCommandInputName() {
        return commandInputName;
    }

    public void setCommandInputName(final String commandInputName) {
        this.commandInputName = commandInputName;
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

    public MatchTreeNode getRoot() {
        return root;
    }

    public void setRoot(final MatchTreeNode root) {
        this.root = root;
    }

    @Transient
    public CommandInput getCommandInput() {
        return parent.getCommandInputByName(commandInputName);
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
        return Objects.equals(this.rootContextPropertyName, that.rootContextPropertyName) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.commandInputName, that.commandInputName) &&
                Objects.equals(this.root, that.root) &&
                Objects.equals(this.parent, that.parent);

    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                rootContextPropertyName, required, commandInputName, root, parent);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rootContextPropertyName", rootContextPropertyName)
                .add("required", required)
                .add("commandInputName", commandInputName)
                .add("root", root)
                .add("parent", parent)
                .toString();
    }

}
