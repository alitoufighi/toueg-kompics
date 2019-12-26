package Events;

import Components.Node;
import Enums.NodeState;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;

public class InitiateMessage implements KompicsEvent {
    public NodeState state;
    public String src, dst;
    public int level, fragmentName;

    public InitiateMessage(String src, String dst, int fragmentName, int level, NodeState state) {
        this.src = src;
        this.dst = dst;
        this.level = level;
        this.fragmentName = fragmentName;
        this.state = state;
    }
}