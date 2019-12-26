package Events;

import se.sics.kompics.KompicsEvent;

public class ConnectMessage implements KompicsEvent {
    public int level;
    public String src, dst;

    public ConnectMessage(String src, String dst, int level) {
        this.src = src;
        this.dst = dst;
        this.level = level;
    }
}
