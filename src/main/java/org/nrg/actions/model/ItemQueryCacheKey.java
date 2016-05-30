package org.nrg.actions.model;

import org.nrg.xft.XFTItem;

import java.util.Objects;

public class ItemQueryCacheKey {
    public ItemQueryCacheKey(final XFTItem item, final String property) {
        this.item = item;
        this.property = property;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ItemQueryCacheKey that = (ItemQueryCacheKey) o;
        return Objects.equals(this.item, that.item) &&
                Objects.equals(this.property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, property);
    }

    private XFTItem item;
    private String property;
}
