package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.actions.model.tree.MatchTreeNode;
import org.nrg.actions.model.tree.MatchTreeNodeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ActionInput implements Serializable {

    private String name;
    @JsonProperty("command-variable") private String commandVariable;
    @JsonProperty("root-context-property-name") private String rootContextPropertyName;
    @JsonIgnore private String rootContextProperty;
    @JsonProperty("match-tree") private MatchTreeNode root;
    private Boolean required;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getCommandVariable() {
        return commandVariable;
    }

    public void setCommandVariable(final String commandVariable) {
        if (this.name == null) {
            this.name = commandVariable;
        }
        this.commandVariable = commandVariable;
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
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.rootContextPropertyName, that.rootContextPropertyName) &&
                Objects.equals(this.required, that.required) &&
                Objects.equals(this.commandVariable, that.commandVariable) &&
                Objects.equals(this.root, that.root);

    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rootContextPropertyName, required, commandVariable, root);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("rootContextPropertyName", rootContextPropertyName)
                .add("required", required)
                .add("commandVariable", commandVariable)
                .add("root", root)
                .toString();
    }

}
