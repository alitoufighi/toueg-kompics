package Components;
import Enums.EdgeState;
import Enums.NodeState;
import Events.*;
import Ports.EdgePort;
import se.sics.kompics.*;
import java.util.*;

class ConnectMessageQueuedItem {
    String node;
    int level;

    ConnectMessageQueuedItem(String node, int level) {
        this.node = node;
        this.level = level;
    }
}

class TestMessageQueuedItem {
    String node;
    int fragmentName;
    int level;

    TestMessageQueuedItem(String node, int fragmentName, int level) {
        this.node = node;
        this.fragmentName = fragmentName;
        this.level = level;
    }

    static TestMessageQueuedItem findByNode(Collection<TestMessageQueuedItem> items, String nodeName) {
        return items.stream().filter(item -> nodeName.equals(item.node)).findFirst().orElse(null);
    }
}

class MinimumSpanningTreeItem {
    private String src, dst;
    private int weight;

    MinimumSpanningTreeItem(String src, String dst, int weight) {
        this.src = src;
        this.dst = dst;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return src + "-" + dst + "," + weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MinimumSpanningTreeItem that = (MinimumSpanningTreeItem) o;
        return weight == that.weight &&
                (Objects.equals(src, that.src) && Objects.equals(dst, that.dst) ||
                        Objects.equals(src, that.dst) && Objects.equals(dst, that.src));
    }
}

public class Node extends ComponentDefinition {
    private static ArrayList<MinimumSpanningTreeItem> mstItems = new ArrayList<>();
    private static boolean writtenInFile = false;
    private static final int maxDistance = 1000;

    private Positive<EdgePort> receivePort = positive(EdgePort.class);
    private Negative<EdgePort> sendPort = negative(EdgePort.class);

    private String p;
    private HashMap<String, Integer> neighbors;

    private HashMap<String, Integer> distances = new HashMap<>();
    private boolean isLeaf;
    private ArrayList<String> leaves = new ArrayList<>();
    private ArrayList<String> nodes;
    private int routesReceivedCount;
    private boolean isRoutingTerminated = false;
    private int childrenStatusReceivedCount = 0;
    private String treeParent;
    private HashMap<String, HashMap<String, Integer>> routingTables = new HashMap<>();
    private boolean isRoot = false;
    private HashMap<String, Integer> leavesInSubtree = new HashMap<>();


    private Handler childrenStatusResponseHandler = new Handler<ChildrenStatusResponseMessage>() {
        @Override
        public void handle(ChildrenStatusResponseMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            childrenStatusReceivedCount++;
            leavesInSubtree.put(message.src, message.leafCount);

            if(isRoot && childrenStatusReceivedCount == neighbors.size() ) {
                for(Map.Entry entry: leavesInSubtree.entrySet()){
                    System.out.println(entry.getKey() + " has " + entry.getValue() + " leaves in subtree");
                }

                //start MapReduce



            }
            else if(childrenStatusReceivedCount == neighbors.size() - 1) {
                sendMessage(new ChildrenStatusResponseMessage(p, treeParent, getSum(leavesInSubtree.values())));
            }
        }
    };

    private Handler childrenStatusRequestHandler = new Handler<ChildrenStatusRequestMessage>() {
        @Override
        public void handle(ChildrenStatusRequestMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            treeParent = message.src;
            if(isLeaf){
                sendMessage(new ChildrenStatusResponseMessage(p, treeParent, 1));
            }
            else {
                for(String neighbor: neighbors.keySet()){
                    if(neighbor.equals(message.src))
                        continue;
                    sendMessage(new ChildrenStatusRequestMessage(p, neighbor));
                }

            }
        }
    };

    private Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            String q = message.src;
        }
    };

    private String findRoot() {
        HashMap<String, Double> avgDistances = new HashMap<>();

        for(Map.Entry<String, HashMap<String, Integer>> item: routingTables.entrySet()){
            double nodeAvgDistance = (double)getSum(item.getValue().values()) / nodes.size();
            avgDistances.put(item.getKey(), nodeAvgDistance);
        }

        double minDistance = Collections.min(avgDistances.entrySet(), Map.Entry.comparingByValue()).getValue();
        ArrayList<String> minDistanceNodes = new ArrayList<>();
        for(Map.Entry<String, Double> entry: avgDistances.entrySet()){
            if(entry.getValue().equals(minDistance)) {
                minDistanceNodes.add(entry.getKey());
            }
        }
        minDistanceNodes.sort(String::compareToIgnoreCase);
        return minDistanceNodes.get(0);
    }

    private Handler routingTableHandler = new Handler<RoutingTableMessage>() {
        @Override
        public void handle(RoutingTableMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            if(routingTables.containsKey(message.node))
                return;
            routingTables.put(message.node, message.routingTable);
            for(String neighbor: neighbors.keySet()){
                if(neighbor.equals(message.src)){
                    continue;
                }
                sendMessage(new RoutingTableMessage(p, neighbor, message.node, message.routingTable, message.isLeaf));
                if(message.isLeaf){
                    leaves.add(message.node);
                }
            }

            if(routingTables.size() == nodes.size()) {
                String root = findRoot();

                if(root.equals(p)) {
                    isRoot = true;
                }

                if(isRoot) {
                    System.out.println("I AM ROOT: " + p);
                    for(String neighbor: neighbors.keySet()){
                        sendMessage(new ChildrenStatusRequestMessage(p, neighbor));
                    }
                }
            }
        }
    };

    private Handler routingHandler = new Handler<RoutingMessage>(){
        @Override
        public void handle(RoutingMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            if(isRoutingTerminated) {
                return;
            }
            HashMap<String, Integer> receivedRoutes = new HashMap<>(message.routes);
            // updating both receivedRoutes and this node's routes
            for(Map.Entry<String, Integer> routingItem: message.routes.entrySet()) {
                if(distances.get(routingItem.getKey()) > distances.get(message.src) + routingItem.getValue()) {
                    distances.replace(routingItem.getKey(), distances.get(message.src) + routingItem.getValue());
                    receivedRoutes.replace(routingItem.getKey(), distances.get(message.src) + routingItem.getValue());
                }
                else {
                    receivedRoutes.replace(routingItem.getKey(), distances.get(routingItem.getKey()));
                }
            }

            // sending updated receivedRoutes to neighbors
            for(String neighbor: neighbors.keySet()){
                if(neighbor.equals(message.src))
                    continue;
                sendMessage(new RoutingMessage(p, neighbor, message.initializer, receivedRoutes));
            }

            routesReceivedCount++;

            if(routesReceivedCount == nodes.size()) {
                isRoutingTerminated = true;

                for(String neighbor: neighbors.keySet()){
                    sendMessage(new RoutingTableMessage(p, neighbor, p, distances, isLeaf));
                }
                routingTables.put(p, distances);


            }

        }
    };

    private int getSum(Collection<Integer> c) {
        return c.stream().mapToInt(Integer::intValue).sum();
    }

    private Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            for (String neighbor: neighbors.keySet()) {
                sendMessage(new RoutingMessage(p, neighbor, p, distances));
            }
        }
    };

    private Handler stopHandler = new Handler<Kill>() {
        @Override
        public void handle(Kill event) {

        }
    };

    private void sendMessage(KompicsEvent event) {
        trigger(event, sendPort);
    }

    public Node(InitMessage initMessage) {
        p = initMessage.nodeName;
        neighbors = initMessage.neighbors;
        nodes = initMessage.nodesList;
        routesReceivedCount = 1;
        isLeaf = (neighbors.size() == 1);

        for (String node: nodes) {
            distances.put(node, neighbors.getOrDefault(node, maxDistance));
        }
        distances.replace(p, 0);

        if(isLeaf) {
            leaves.add(p);
        }

        subscribe(startHandler, control);
        subscribe(stopHandler, control);
        subscribe(routingHandler, receivePort);
        subscribe(routingTableHandler, receivePort);
        subscribe(childrenStatusRequestHandler, receivePort);
        subscribe(childrenStatusResponseHandler, receivePort);
    }
}

