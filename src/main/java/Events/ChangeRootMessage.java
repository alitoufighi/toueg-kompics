package Events;

import se.sics.kompics.KompicsEvent;

public class ChangeRootMessage implements KompicsEvent {
    public String src, dst;

    public ChangeRootMessage(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }
}
