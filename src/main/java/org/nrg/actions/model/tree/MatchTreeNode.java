package org.nrg.actions.model.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.actions.model.matcher.Matcher;

import javax.persistence.ElementCollection;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class MatchTreeNode implements Serializable {
    private String type;
    private MatchTreeNode parent;
    private List<MatchTreeNode> children;
    private List<Matcher> matchers;
    @JsonProperty("provides-input-value") private Boolean providesInputValue;
    @JsonProperty("input-property") private String inputProperty;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public MatchTreeNode getParent() {
        return parent;
    }

    public void setParent(final MatchTreeNode parent) {
        this.parent = parent;
    }

    @ElementCollection
    public List<MatchTreeNode> getChildren() {
        return children;
    }

    public void setChildren(final List<MatchTreeNode> children) {
        this.children = children;
    }

    @ElementCollection
    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(final List<Matcher> matchers) {
        this.matchers = matchers;
    }

    public Boolean getProvidesInputValue() {
        return providesInputValue;
    }

    public void setProvidesInputValue(final Boolean input) {
        this.providesInputValue = input;
    }

    public String getInputProperty() {
        return inputProperty;
    }

    public void setInputProperty(final String inputProperty) {
        this.inputProperty = inputProperty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, parent, children, matchers, providesInputValue, inputProperty);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MatchTreeNode that = (MatchTreeNode) o;

        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.parent, that.parent) &&
                Objects.equals(this.children, that.children) &&
                Objects.equals(this.matchers, that.matchers) &&
                Objects.equals(this.providesInputValue, that.providesInputValue) &&
                Objects.equals(this.inputProperty, that.inputProperty);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("parent", parent)
                .add("children", children)
                .add("matchers", matchers)
                .add("input", providesInputValue)
                .add("inputProperty", inputProperty)
                .toString();
    }
}
