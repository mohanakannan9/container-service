//package org.nrg.actions.services;
//
//import com.google.common.base.Function;
//import com.google.common.collect.Lists;
//import org.nrg.actions.model.Action;
//import org.nrg.actions.model.ActionContextExection;
//import org.nrg.actions.model.CommandVariable;
//import org.nrg.actions.model.Context;
//import org.nrg.actions.model.ActionInput;
//import org.nrg.actions.model.tree.RuntimeTree;
//import org.nrg.actions.model.tree.RuntimeTreeNode;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Nullable;
//import java.util.List;
//
//@Service
//public class ActionContextExecutionResolverService {
//
//    public ActionContextExection resolve(final Context context, final Action action) {
//
//        // Get required properties from context
//        final List<ActionInput> toResolve = Lists.newArrayList();
//        for (final ActionInput actionInput : action.getInputs()) {
//            final CommandVariable commandLineInput = actionInput.getCommandInput();
//            final String rootContextPropertyName = actionInput.getRootContextPropertyName();
//            if (!context.containsKey(rootContextPropertyName)) {
//                if (commandLineInput.isRequired()) {
//                    // TODO if input is required and context does not have the property, throw error
//                }
//                continue;
//            }
//
//            final String contextProperty = context.get(rootContextPropertyName);
//            actionInput.setRootContextProperty(contextProperty);
//            toResolve.add(actionInput);
//        }
//
//        final List<RuntimeTree> runtimeTrees = resolve(toResolve);
//
//        // TODO make an ACE from the runtime trees and whatever else an ACE needs
//        return new ActionContextExection();
//    }
//
//    private List<RuntimeTree> resolve(final List<ActionInput> actionInputs) {
//        return Lists.transform(actionInputs, new Function<ActionInput, RuntimeTree>() {
//            @Nullable
//            @Override
//            public RuntimeTree apply(@Nullable final ActionInput input) {
//                return resolve(input);
//            }
//        });
//    }
//
//    private RuntimeTree resolve(final ActionInput actionInput) {
//        // Get type of root MTNode
//        // Now we know type of RTNode to create
//        final String rootMatchTreeType = actionInput.getRoot().getType();
//
//        final String rootContextProperty = actionInput.getRootContextProperty();
//
//
//        // identifier = context.get(action.matchtree.rootcontextproperty)
//
//        // TODO RuntimeTreeNode<type> rtNode;
//        RuntimeTreeNode rtNode;
//
//        RuntimeTree runtimeTree = null;
//        return runtimeTree;
//    }
//}
