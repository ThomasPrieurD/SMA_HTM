package htm;

import graph.NodeInterface;

public class MyCell extends MyNeuron {


    public MyCell(NodeInterface _node, int activation) {
        super(_node, activation);
    }

    public MyCell(NodeInterface _node) {
        super(_node);
    }
}
