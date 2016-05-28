package org.nrg.actions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.nrg.actions.model.matcher.Matcher;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;
import java.util.Objects;

@Embeddable
public class ActionRoot {

    @JsonProperty("name") private String rootName;
    private String xsiType;
    private List<Matcher> matchers;

    public String getRootName() {
        return rootName;
    }

    public void setRootName(final String rootName) {
        this.rootName = rootName;
    }

    public String getXsiType() {
        return xsiType;
    }

    public void setXsiType(final String xsiType) {
        this.xsiType = xsiType;
    }

    @ElementCollection
    public List<Matcher> getMatchers() {
        return matchers;
    }

    public void setMatchers(final List<Matcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActionRoot that = (ActionRoot) o;
        return Objects.equals(this.rootName, that.rootName) &&
                Objects.equals(this.xsiType, that.xsiType) &&
                Objects.equals(this.matchers, that.matchers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootName, xsiType, matchers);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", rootName)
                .add("xsiType", xsiType)
                .add("matchers", matchers)
                .toString();
    }
}
