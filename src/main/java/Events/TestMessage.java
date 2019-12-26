package Events;

import se.sics.kompics.KompicsEvent;

public class TestMessage implements KompicsEvent {
    public String src, dst;
    public int level, fragmentName;

    public TestMessage(String src, String dst, int level, int fragmentName) {
        this.src = src;
        this.dst = dst;
        this.level = level;
        this.fragmentName = fragmentName;
    }
}
