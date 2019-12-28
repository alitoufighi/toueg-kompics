package Events;


import misc.TableRow;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class RoutingMessage implements KompicsEvent {
    public String src, dst, initializer;
    public HashMap<String, Integer> routes;

    public RoutingMessage(String src, String dst, String initializer, HashMap<String, Integer> routes) {
        this.src = src;
        this.dst = dst;
        this.initializer = initializer;
        this.routes = routes;
    }
}
