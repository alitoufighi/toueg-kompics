package Ports;

import Events.*;
import se.sics.kompics.PortType;

public class EdgePort extends PortType {{
    positive(AcceptMessage.class);
    positive(ReportMessage.class);
    positive(ChangeRootMessage.class);
    positive(ConnectMessage.class);
    positive(InitiateMessage.class);
    positive(RejectMessage.class);
    positive(TestMessage.class);
    negative(AcceptMessage.class);
    negative(ReportMessage.class);
    negative(ChangeRootMessage.class);
    negative(ConnectMessage.class);
    negative(InitiateMessage.class);
    negative(RejectMessage.class);
    negative(TestMessage.class);
}}
