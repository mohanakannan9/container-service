package org.nrg.containers.model.xnat;

import com.google.common.base.MoreObjects;

import java.util.Objects;

public class InnerTestPojo {
    private String innerKey1;
    private String innerKey2;

    public InnerTestPojo() {}

    public InnerTestPojo(final String innerKey1, final String innerKey2) {
        this.innerKey1 = innerKey1;
        this.innerKey2 = innerKey2;
    }

    public String getInnerKey1() {
        return innerKey1;
    }

    public void setInnerKey1(final String innerKey1) {
        this.innerKey1 = innerKey1;
    }


    public String getInnerKey2() {
        return innerKey2;
    }

    public void setInnerKey2(final String innerKey2) {
        this.innerKey2 = innerKey2;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final InnerTestPojo that = (InnerTestPojo) o;
        return Objects.equals(this.innerKey1, that.innerKey1) &&
                Objects.equals(this.innerKey2, that.innerKey2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innerKey1, innerKey2);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("innerKey1", innerKey1)
                .add("innerKey2", innerKey2)
                .toString();
    }
}
