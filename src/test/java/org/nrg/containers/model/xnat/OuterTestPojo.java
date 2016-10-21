package org.nrg.containers.model.xnat;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class OuterTestPojo {
    private InnerTestPojo outerKey1;

    public InnerTestPojo getOuterKey1() {
        return outerKey1;
    }

    public void setOuterKey1(final InnerTestPojo outerKey1) {
        this.outerKey1 = outerKey1;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final OuterTestPojo that = (OuterTestPojo) o;
        return Objects.equals(this.outerKey1, that.outerKey1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outerKey1);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key1", outerKey1)
                .toString();
    }
}
