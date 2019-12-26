package Components;
import Enums.EdgeState;
import Enums.NodeState;
import Events.ConnectMessage;
import Events.InitMessage;
import Events.ReportMessage;
import Events.RoutingMessage;
import Ports.EdgePort;
import misc.TableRow;
import se.sics.kompics.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;


public class Node extends ComponentDefinition {
    private Positive<EdgePort> receivePort = positive(EdgePort.class);
    private Negative<EdgePort> sendPort = negative(EdgePort.class);

    private String name;
    private String parent;
    private String testEdge;
    private String bestEdge;
    private int level;
    private int counter;
    private int bestWeight;
    private int parentReport;
    private NodeState nodeState;
    private ArrayList<String> tests = new ArrayList<>();
    private ArrayList<String> connects = new ArrayList<>();
    private HashMap<String,Integer> edgeWeights = new HashMap<>();
    private HashMap<String, EdgeState> edgeStates = new HashMap<>();

    private Handler routingHandler = new Handler<RoutingMessage>(){
        @Override
        public void handle(RoutingMessage event) {
            if (name.equalsIgnoreCase(event.dst)){
                System.out.println(name +  " recieved message : src " + event.src + " dst " + event.dst);
                if (dist > event.weight){
                    dist = event.weight;
                    parent = event.src;
                    trigger(new ReportMessage(name,parent ,dist, route_table),sendPort);
                    System.out.println(String.format("node %s dist is: %s",name,dist));
                    System.out.println(String.format("node %s parent is: %s",name,parent));
                    for( Map.Entry<String, Integer> entry : edgeWeights.entrySet())
                    {
                        if(!entry.getKey().equalsIgnoreCase(parent))
                        {
                            trigger(new RoutingMessage(name,entry.getKey() ,dist + entry.getValue(),entry.getValue()),sendPort);
                        }
                    }
                }
            }
        }
    };


    private Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage event) {
            if (name.equalsIgnoreCase(event.dst))
            {
                ArrayList<TableRow> newRoute = new ArrayList<>();
                newRoute.add(new TableRow(event.src,event.src, event.dist));
                for(TableRow tr:event.route_table){
                    tr.first_node = event.src;
                    newRoute.add(tr);
                }
                for(TableRow tr:route_table){
                    boolean remove = false;
                    for(TableRow t:newRoute){
                        if(tr.dst.equals(t.dst)){
                            remove = true;
                        }
                    }
                    if(!remove){
                        newRoute.add(tr);
                    }
                }
                route_table = newRoute;
                if (parent!=null)
                    trigger(new ReportMessage(name,parent,dist ,route_table),sendPort);
                Path path = Paths.get("src/main/java/Routes/table" + name + ".txt");
                OpenOption[] options = new OpenOption[] { WRITE , CREATE};
                try {
                    Files.write(path,route_table.toString().getBytes(),options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private String getLowestWeightEdge() {
        return Collections.min(edgeWeights.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private Handler connectHandler = new Handler<ConnectMessage>(){
        @Override
        public void handle(ConnectMessage connectMessage) {
            if (!name.equalsIgnoreCase(connectMessage.getDestination()))
                return;
            if(level > connectMessage.getLevel()) {

            }
            System.out.println(name +  " recieved message : src " + event.getSource() + " dst " + event.getDestination());
        }
    };

    private Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            String minWeightEdge = getLowestWeightEdge();

            nodeState = NodeState.FOUND;
            edgeStates.put(minWeightEdge, EdgeState.BRANCH);
            counter = 1;
            parentReport = 0;

            sendMessage(new ConnectMessage(name, minWeightEdge, 0));
        }
    };

    private void sendMessage(KompicsEvent event) {
        trigger(event, sendPort);
    }

    public Node(InitMessage initMessage) {
        name = initMessage.nodeName;
        System.out.println("initNode :" + initMessage.nodeName);
        this.edgeWeights = initMessage.neighbours;
//        this.isRoot = initMessage.isRoot;
        subscribe(startHandler, control);
        subscribe(reportHandler, receivePort);
        subscribe(routingHandler, receivePort);
    }


}

