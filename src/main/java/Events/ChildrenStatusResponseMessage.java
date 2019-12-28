package Events;

import se.sics.kompics.KompicsEvent;

public class ChildrenStatusResponseMessage implements KompicsEvent {
    public String src, dst;
    public int leafCount;

    public ChildrenStatusResponseMessage(String src, String dst, int leafCount) {
        this.src = src;
        this.dst = dst;
        this.leafCount = leafCount;
    }
}
