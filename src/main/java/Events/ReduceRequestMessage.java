package Events;

import se.sics.kompics.KompicsEvent;

import java.util.HashMap;

public class ReduceRequestMessage implements KompicsEvent {
    public String src, dst;
    public HashMap<String, Integer> data;

    public ReduceRequestMessage(String src, String dst, HashMap<String, Integer> data) {
        this.src = src;
        this.dst = dst;
        this.data = data;
    }
}
