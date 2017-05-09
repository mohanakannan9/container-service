package org.nrg.containers.model.command.auto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.Input;

import java.util.List;

@AutoValue
public abstract class ResolvedInputTreeNode<T extends Input> {
    @JsonProperty("input") public abstract T input();
    @JsonProperty("values-and-children") public abstract List<ResolvedInputTreeValueAndChildren> valuesAndChildren();

    public static ResolvedInputTreeNode<? extends Input> create(final PreresolvedInputTreeNode preresolvedInputTreeNode) {
        final Input input = preresolvedInputTreeNode.input();
        if (input instanceof CommandWrapperExternalInput) {
            return create((CommandWrapperExternalInput) input);
        } else if (input instanceof CommandWrapperDerivedInput) {
            return create((CommandWrapperDerivedInput) input);
        } else {
            return create((CommandInput) input);
        }
    }

    static ResolvedInputTreeNode<CommandWrapperExternalInput> create(final CommandWrapperExternalInput externalInput) {
        return new AutoValue_ResolvedInputTreeNode<>(externalInput, Lists.<ResolvedInputTreeValueAndChildren>newArrayList());
    }

    static ResolvedInputTreeNode<CommandWrapperDerivedInput> create(final CommandWrapperDerivedInput derivedInput) {
        return new AutoValue_ResolvedInputTreeNode<>(derivedInput, Lists.<ResolvedInputTreeValueAndChildren>newArrayList());
    }

    static ResolvedInputTreeNode<CommandInput> create(final CommandInput commandInput) {
        return new AutoValue_ResolvedInputTreeNode<>(commandInput, Lists.<ResolvedInputTreeValueAndChildren>newArrayList());
    }

    @JsonCreator
    public static <X extends Input> ResolvedInputTreeNode<X> create(@JsonProperty("input") final X input,
                                                                    @JsonProperty("values-and-children") final List<ResolvedInputTreeValueAndChildren> valuesAndChildren) {
        return new AutoValue_ResolvedInputTreeNode<>(input,
                valuesAndChildren == null ? Lists.<ResolvedInputTreeValueAndChildren>newArrayList() : valuesAndChildren);
    }

    @AutoValue
    public static abstract class ResolvedInputTreeValueAndChildren {
        @JsonProperty("value") public abstract ResolvedInputValue resolvedValue();
        @JsonProperty("children") public abstract List<ResolvedInputTreeNode<? extends Input>> children();

        public static ResolvedInputTreeValueAndChildren create(final ResolvedInputValue resolvedValue) {
            return new AutoValue_ResolvedInputTreeNode_ResolvedInputTreeValueAndChildren(resolvedValue, Lists.<ResolvedInputTreeNode<? extends Input>>newArrayList());
        }

        @JsonCreator
        public static ResolvedInputTreeValueAndChildren create(@JsonProperty("value") final ResolvedInputValue resolvedValue,
                                                               @JsonProperty("children") final List<ResolvedInputTreeNode<? extends Input>> children) {
            return new AutoValue_ResolvedInputTreeNode_ResolvedInputTreeValueAndChildren(resolvedValue,
                    children == null ? Lists.<ResolvedInputTreeNode<? extends Input>>newArrayList() : children);
        }
    }
}
