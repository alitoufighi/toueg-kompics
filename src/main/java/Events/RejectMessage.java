package Events;

import se.sics.kompics.KompicsEvent;

public class RejectMessage implements KompicsEvent {
    public String src, dst;

    public RejectMessage(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
