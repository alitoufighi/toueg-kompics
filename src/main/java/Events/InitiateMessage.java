package Events;

import Components.Node;
import Enums.NodeState;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;

public class InitiateMessage implements KompicsEvent {
    private String src, dst;

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

    public int getLevel() {
        return level;
    }

    public NodeState getState() {
        return state;
    }

    private int level;

    public InitiateMessage(String src, String dst, int level, NodeState state) {
        this.src = src;
        this.dst = dst;
        this.level = level;
        this.state = state;
    }

    private NodeState state;
}