package Components;
import Enums.EdgeState;
import Enums.NodeState;
import Events.*;
import Ports.EdgePort;
import se.sics.kompics.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

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

    private Positive<EdgePort> receivePort = positive(EdgePort.class);
    private Negative<EdgePort> sendPort = negative(EdgePort.class);

    private String p;
    private String parent;
    private String testEdge;
    private String bestEdge;
    private int level;
    private int counter;
    private int bestWeight;
    private int parentReport;
    private int fragmentName;
    private NodeState nodeState;
    private ArrayList<TestMessageQueuedItem> tests = new ArrayList<>();
    private ArrayList<ConnectMessageQueuedItem> connects = new ArrayList<>();
    private HashMap<String, Integer> edgeWeights = new HashMap<>();
    private HashMap<String, EdgeState> edgeStates = new HashMap<>();

    private void addToMstListIfNotPresent(MinimumSpanningTreeItem mstItem) {
        if(!mstItems.contains(mstItem)) {
            mstItems.add(mstItem);
        }
    }

    private void addEdgeToMst(String q, int weight) {
        edgeStates.put(q, EdgeState.BRANCH);
        addToMstListIfNotPresent(new MinimumSpanningTreeItem(p, q, weight));
    }

    private Handler testHandler = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage testMessage) {
            String q = testMessage.src;
            if (!p.equalsIgnoreCase(q))
                return;
            if(level >= testMessage.level) {
                replyTest(q);
            }
            else {
                tests.add(new TestMessageQueuedItem(q, testMessage.fragmentName, testMessage.level));
            }
        }
    };

    private Handler rejectHandler = new Handler<RejectMessage>() {
        @Override
        public void handle(RejectMessage rejectMessage) {
            String q = rejectMessage.src;
            if (!p.equalsIgnoreCase(q))
                return;
            edgeStates.put(q, EdgeState.REJECTED);
            findMinimalOutgoing();
        }
    };

    private Handler acceptHandler = new Handler<AcceptMessage>() {
        @Override
        public void handle(AcceptMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            String q = message.src;
            testEdge = null;
            if(edgeWeights.get(q) < bestWeight) {
                bestEdge = q;
                bestWeight = edgeWeights.get(q);
            }
            if(counter == Collections.frequency(edgeStates.values(), EdgeState.BRANCH)){
                sendReport();
            }
        }
    };

    private Handler changeRootHandler = new Handler<ChangeRootMessage>() {
        @Override
        public void handle(ChangeRootMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            changeRoot();
        }
    };

    private Handler reportHandler = new Handler<ReportMessage>() {
        @Override
        public void handle(ReportMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            String q = message.src;
            if(!parent.equals(q)) {
                counter++;
                if(message.weight < bestWeight) {
                    bestEdge = q;
                    bestWeight = message.weight;
                }
                if(counter == Collections.frequency(edgeStates.values(), EdgeState.BRANCH)){
                    sendReport();
                }
            }
            else if (nodeState == NodeState.FIND) {
                parentReport = message.weight;
            }
            else {
                if (message.weight > bestWeight) {
                    changeRoot();
                }
                else if(message.weight == Integer.MAX_VALUE){
                    terminate();
                }
            }
        }
    };

    private Handler connectHandler = new Handler<ConnectMessage>() {
        @Override
        public void handle(ConnectMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            String q = message.src;
            if(level > message.level) {
                sendMessage(new InitiateMessage(p, q, fragmentName, level, nodeState));

                addEdgeToMst(q, edgeWeights.get(q));
//                edgeStates.put(q, EdgeState.BRANCH);

//                System.out.println("  CONN: NEW BRANCH EDGE FROM " + p + " TO " + q);
            }
            else if (edgeStates.get(q).equals(EdgeState.BRANCH)){
                sendMessage(new InitiateMessage(p, q, edgeWeights.get(q), level+1, NodeState.FIND));
            }
            else {
                connects.add(new ConnectMessageQueuedItem(q, level));
            }
        }
    };

    private Handler initiateHandler = new Handler<InitiateMessage>(){
        @Override
        public void handle(InitiateMessage message) {
            if (!p.equalsIgnoreCase(message.dst))
                return;
            String q = message.src;
            fragmentName = message.fragmentName;
            level = message.level;
            nodeState = message.state;
            parent = q;
            bestEdge = null;
            bestWeight = Integer.MAX_VALUE;
            counter = 1;
            parentReport = 0;

            for (ConnectMessageQueuedItem connectRequest: new ArrayList<>(connects)) {
                if(level > connectRequest.level) {
//                    edgeStates.put(connectRequest.node, EdgeState.BRANCH);
                    addEdgeToMst(connectRequest.node, edgeWeights.get(connectRequest.node));
//                    System.out.println("  INIT: NEW BRANCH EDGE FROM " + p + " TO " + q);
                    connects.remove(connectRequest);
                }
            }

            for (Map.Entry<String, EdgeState> entry: edgeStates.entrySet()) {
                if(!entry.getKey().equals(q) && entry.getValue().equals(EdgeState.BRANCH)){
                    sendMessage(new InitiateMessage(p, entry.getKey(), fragmentName, level, nodeState));
                }
            }

            for(TestMessageQueuedItem testRequest: new ArrayList<>(tests)) {
                if(level >= testRequest.level) {
                    replyTest(testRequest.node);
                    tests.remove(testRequest);
                }
            }

            if(nodeState == NodeState.FIND) {
                findMinimalOutgoing();
            }
        }
    };

    private Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            String minWeightEdge = getLowestWeightBasicEdge();
//            System.out.println("Min Weight Edge for " + p + " is " + minWeightEdge);
            nodeState = NodeState.FOUND;
            addEdgeToMst(minWeightEdge, edgeWeights.get(minWeightEdge));
//            edgeStates.put(minWeightEdge, EdgeState.BRANCH);
//            System.out.println(" START: NEW BRANCH EDGE FROM " + p + " TO " + minWeightEdge);
            counter = 1;
            parentReport = 0;

            sendMessage(new ConnectMessage(p, minWeightEdge, 0));
        }
    };

    private Handler stopHandler = new Handler<Kill>() {
        @Override
        public void handle(Kill event) {
            if(!writtenInFile) {
                new File(Paths.get("output.txt").toString()).delete();
                System.out.println("Printing MST edges to output.txt file...");
                try{
                    for (MinimumSpanningTreeItem item: mstItems) {
                        String line = item.toString();
                        Files.write(Paths.get("output.txt"),
                                (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                } catch (IOException e){
                    System.out.println("Exception occurred.");
                }
                writtenInFile = true;
            }
//            System.out.println("For Node " + p);
//            for (Map.Entry entry: edgeStates.entrySet()) {
//                if(entry.getValue().equals(EdgeState.BRANCH)){
//                    System.out.print(entry.getKey() + " ");
//                }
//            }
//            System.out.println();
        }
    };

    private void replyTest(String q) {
        TestMessageQueuedItem testRequest = TestMessageQueuedItem.findByNode(tests, q);
        if(fragmentName != testRequest.fragmentName) {
            sendMessage(new AcceptMessage(p, q));
        }
        else {
            edgeStates.put(q, EdgeState.REJECTED);
            if(!testEdge.equals(q)) {
                sendMessage(new RejectMessage(p, q));
            }
            else {
                findMinimalOutgoing();
            }
        }
    }

    private void findMinimalOutgoing() {
        if(edgeStates.containsValue(EdgeState.BASIC)){
            String q = getLowestWeightBasicEdge();
            sendMessage(new TestMessage(p, q, fragmentName, level));
            testEdge = q;
        }
        else {
            testEdge = null;
        }
    }

    private void terminate() {
        System.out.println("TERMINATOR NODE: " + p);
    }

    private void changeRoot() {
        if (edgeStates.get(bestEdge).equals(EdgeState.BRANCH)){
            sendMessage(new ChangeRootMessage(p, bestEdge));
        }
        else {
            addEdgeToMst(bestEdge, edgeWeights.get(bestEdge));
//            edgeStates.put(bestEdge, EdgeState.BRANCH);
//            System.out.println("CHROOT: NEW BRANCH EDGE FROM " + p + " TO " + bestEdge);

            sendMessage(new ConnectMessage(p, bestEdge, level));
            ConnectMessageQueuedItem item = new ConnectMessageQueuedItem(bestEdge, level); //TODO: Name?
            if(connects.contains(item)){
                sendMessage(new InitiateMessage(p, bestEdge, bestWeight, level+1, NodeState.FIND));
                connects.remove(item);
            }
        }
    }

    private void sendReport() {
        nodeState = NodeState.FOUND;
        sendMessage(new ReportMessage(p, parent, bestWeight));
        if(parentReport > 0 && bestWeight < parentReport) {
            changeRoot();
        }
    }

    private String getLowestWeightBasicEdge() {
        HashMap<String, EdgeState> basicEdges = edgeStates.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(EdgeState.BASIC))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));

        HashMap<String, Integer> basicEdgesWithWeight = edgeWeights.entrySet()
                .stream()
                .filter(entry -> basicEdges.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));

        return Collections.min(basicEdgesWithWeight.entrySet(), Map.Entry.comparingByValue()).getKey();
    }


    private void sendMessage(KompicsEvent event) {
        trigger(event, sendPort);
    }

    public Node(InitMessage initMessage) {
        p = initMessage.nodeName;
        edgeWeights = initMessage.neighbours;
        nodeState = NodeState.SLEEP;
        for(String neighbour : edgeWeights.keySet())
            edgeStates.put(neighbour, EdgeState.BASIC);

        subscribe(startHandler, control);
        subscribe(stopHandler, control);
        subscribe(reportHandler, receivePort);
        subscribe(rejectHandler, receivePort);
        subscribe(acceptHandler, receivePort);
        subscribe(changeRootHandler, receivePort);
        subscribe(reportHandler, receivePort);
        subscribe(connectHandler, receivePort);
        subscribe(initiateHandler, receivePort);
    }
}

