package Events;

import se.sics.kompics.KompicsEvent;

public class AcceptMessage implements KompicsEvent {
    public String src, dst;

    public AcceptMessage(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
