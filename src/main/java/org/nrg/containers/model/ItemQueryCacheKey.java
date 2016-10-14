package org.nrg.containers.model;

import org.nrg.xft.ItemI;

import java.util.Objects;

public class ItemQueryCacheKey {
    public ItemQueryCacheKey(final ItemI itemI, final String property) {
        this.itemI = itemI;
        this.property = property;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ItemQueryCacheKey that = (ItemQueryCacheKey) o;
        return Objects.equals(this.itemI, that.itemI) &&
                Objects.equals(this.property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemI, property);
    }

    private ItemI itemI;
    private String property;
}
