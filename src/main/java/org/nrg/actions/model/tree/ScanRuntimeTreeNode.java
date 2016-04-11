package org.nrg.actions.model.tree;

import org.nrg.xdat.om.XnatImagescandata;

public class ScanRuntimeTreeNode<T> extends RuntimeTreeNode<T> {
    private XnatImagescandata scan;

    public ScanRuntimeTreeNode(final MatchTreeNode mtNode, final String identifier) {
//        this.scan = XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(identifier, );
        // TODO
    }

    public ScanRuntimeTreeNode(final MatchTreeNode mtNode, final XnatImagescandata scan) {
        this.scan = scan;
        // TODO create children using info in mtNode and mtNode.children
        // are all children valid? If so, I am valid. If not, just return.
        // do any children have a value? If so, use it for my value (or maybe do something to it first).
    }

    @Override
    public void makeChildren() {
        //TODO for each mtNode in mtNode.children, switch based on type of mtNode
    }

    public void makeChild(final MatchTreeNode mtNode) {
//        switch (mtNode.type()) {
//            case "Resource":
////                scan.getFile()
//                break;
//            case "String":
//                scan.getProperty()
//                break;
//            default:
//                throw new RuntimeException("Cannot make a child of type " + mtNode.type());
//        }
    }
}
