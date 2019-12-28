package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(RoutingMessage.class);
    positive(RoutingTableMessage.class);
    positive(ChildrenStatusRequestMessage.class);
    positive(ChildrenStatusResponseMessage.class);

    negative(RoutingMessage.class);
    negative(RoutingTableMessage.class);
    negative(ChildrenStatusRequestMessage.class);
    negative(ChildrenStatusResponseMessage.class);
}}
