package org.nrg.actions.model.tree;

import java.util.List;

public abstract class RuntimeTreeNode<T> {
    private RuntimeTreeNode<T> parent;
    private List<RuntimeTreeNode<T>> children;
    private T value;
    private Boolean input;

    abstract void makeChildren();
}
