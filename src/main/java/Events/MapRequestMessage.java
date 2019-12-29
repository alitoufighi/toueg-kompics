package Events;

import se.sics.kompics.KompicsEvent;

public class MapRequestMessage implements KompicsEvent {
    public String src, dst;
    public String fileName;
    public int startPoint, endPoint;

    public MapRequestMessage(String src, String dst, String fileName, int startPoint, int endPoint) {
        this.src = src;
        this.dst = dst;
        this.fileName = fileName;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }
}
