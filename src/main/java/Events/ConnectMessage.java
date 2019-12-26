package Events;

import se.sics.kompics.KompicsEvent;

public class ConnectMessage implements KompicsEvent {
    private int level;
    private String src, dst;

    public int getLevel() {
        return level;
    }

    public String getSource() {
        return src;
    }

    public String getDestination() {
        return dst;
    }



    public ConnectMessage(String src, String dst, int level) {
        this.src = src;
        this.dst = dst;
        this.level = level;
    }
}
