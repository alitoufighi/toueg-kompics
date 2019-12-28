package Events;

import Components.Node;
import se.sics.kompics.Init;
import java.util.ArrayList;
import java.util.HashMap;

public class InitMessage extends Init<Node> {
    public String nodeName;
    public HashMap<String,Integer> neighbors = new HashMap<>();
    public ArrayList<String> nodesList;

    public InitMessage(String nodeName, HashMap<String, Integer> neighbours, ArrayList<String> nodes) {
        this.nodeName = nodeName;
        this.neighbors = neighbours;
        this.nodesList = nodes;
    }
}