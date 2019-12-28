package Events;

import se.sics.kompics.KompicsEvent;

import java.util.HashMap;

public class RoutingTableMessage implements KompicsEvent {
    public String src, dst, node;
    public HashMap<String, Integer> routingTable;
    public boolean isLeaf;

    public RoutingTableMessage(String src, String dst, String node,
                               HashMap<String, Integer> routingTable, boolean isLeaf) {
        this.src = src;
        this.dst = dst;
        this.node = node;
        this.routingTable = routingTable;
        this.isLeaf = isLeaf;
    }
}
