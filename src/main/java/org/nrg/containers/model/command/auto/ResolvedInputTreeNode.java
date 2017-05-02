package org.nrg.containers.model.command.auto;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.Input;

import java.util.List;

@AutoValue
public abstract class ResolvedInputTreeNode<T extends Input> {
    public abstract T input();
    public abstract List<ResolvedInputTreeValueAndChildren> inputValuesAndChildren();

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

    @AutoValue
    public static abstract class ResolvedInputTreeValueAndChildren {
        public abstract ResolvedInputValue resolvedValue();
        public abstract List<ResolvedInputTreeNode<? extends Input>> children();

        public static ResolvedInputTreeValueAndChildren create(final ResolvedInputValue resolvedValue) {
            return new AutoValue_ResolvedInputTreeNode_ResolvedInputTreeValueAndChildren(resolvedValue, Lists.<ResolvedInputTreeNode<? extends Input>>newArrayList());
        }

        public static ResolvedInputTreeValueAndChildren create(final ResolvedInputValue resolvedValue,
                                                               final List<ResolvedInputTreeNode<? extends Input>> children) {
            return new AutoValue_ResolvedInputTreeNode_ResolvedInputTreeValueAndChildren(resolvedValue, children);
        }
    }
}
