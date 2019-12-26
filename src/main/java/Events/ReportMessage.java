package Events;


import misc.TableRow;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;

public class ReportMessage implements KompicsEvent {
    public String src, dst;
    public int weight;

    public ReportMessage(String src, String dst, int weight) {
        this.src = src;
        this.dst = dst;
        this.weight = weight;
    }
}
