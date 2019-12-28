package Events;

import se.sics.kompics.KompicsEvent;

public class ChildrenStatusRequestMessage implements KompicsEvent {
    public String src, dst;

    public ChildrenStatusRequestMessage(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
