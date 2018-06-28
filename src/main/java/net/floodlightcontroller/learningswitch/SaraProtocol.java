package net.floodlightcontroller.learningswitch;

import org.apache.derby.impl.sql.compile.IntersectOrExceptNode;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.*;

public class SaraProtocol {
    private Map<InEntry, OutEntry> learned = new HashMap<>();
    private Map<Long, Set<OutEntry>> waitingRoom = new HashMap<>();
    private Map<Long, SaraProtocolUtils.SaraProtocolState> switchStates = new HashMap<>();
    private Map<Long, ArrayList<OFPort>> switchPorts = new HashMap<>();
    private Map<Long, ArrayList<OFPort>> sentSwitchPorts = new HashMap<>();
    public int edges = 0;
    public int vertices = 0;
    public boolean MSTMade = false;
    private Graph graph;
    private Graph.Edge[] finalEdges;

    public boolean isLearned(){
        Iterator it = switchStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(pair.getValue() != SaraProtocolUtils.SaraProtocolState.LEARNED){
                return false;
            }
        }
        return true;
    }

    public int getSwitchSize(){
        return switchStates.size();
    }

    public Map<InEntry, OutEntry> getLearned(){
        return learned;
    }

    public void createGraph(){
        graph = new Graph(vertices + switchStates.size(), edges / 2);
        int i = 0;
        Iterator it = learned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            graph.edge[i].src = ((InEntry)pair.getKey());
            graph.edge[i].dest = ((OutEntry)pair.getValue());
            graph.edge[i].weight = ((OutEntry)pair.getValue()).getValue();
        }
        finalEdges = graph.KruskalMST();
    }

    public ArrayList<OFPort> getNextPorts(long sw){
        ArrayList<OFPort> p = new ArrayList<>();
        for(int i = 0 ; i < finalEdges.length ; i++){
            if(finalEdges[i].src.getSw() == sw){
                p.add(finalEdges[i].src.getPort());
            }
            else if(finalEdges[i].dest.getSw() == sw){
                p.add(finalEdges[i].dest.getPort());
            }
        }
        return p;
    }

    public void addPort(long sw, OFPort p){
        ArrayList<OFPort> temp = switchPorts.get(sw);
        if(temp == null){
            temp = new ArrayList<>();
        }
        temp.add(p);
        switchPorts.put(sw, temp);
        sentSwitchPorts.put(sw, temp);
    }

    public OFPort getPort(long sw){
        Random r = new Random();
        int i = r.nextInt(switchPorts.get(sw).size());
        return switchPorts.get(sw).get(i);
    }

    public ArrayList<OFPort> getSentPorts(long sw){
        return sentSwitchPorts.get(sw);
    }

    public void removePort(long sw, OFPort p){
        ArrayList<OFPort> temp = switchPorts.get(sw);
        temp.remove(p);
        switchPorts.put(sw, temp);
    }

    public Long getCurrentSwitch() {
        return currentSwitch;
    }

    private Long currentSwitch;
    private long startBroadcast;

    public boolean haveState(SaraProtocolUtils.SaraProtocolState state){
        return switchStates.containsValue(state);
    }

    public SaraProtocolUtils.SaraProtocolState getState(long sw){
        return switchStates.get(sw);
    }

    public void setState(long sw, SaraProtocolUtils.SaraProtocolState state){
        switchStates.put(sw, state);
    }

    public long getStartBroadcast() {
        return startBroadcast;
    }

    public void setStartBroadcast() {
        this.startBroadcast = System.currentTimeMillis();
    }

//    public boolean haveEntryFor(long id) {
//        // todo
//        return false;
//    }

    public void setCurrentSwitch(long currentSwitch) {
        this.currentSwitch = currentSwitch;
    }

    public void makeEntryFor(long currentSwitchId) {
        waitingRoom.put(currentSwitchId,new HashSet<>());
        // todo
    }

    private class Entry {
        private long sw;
        private OFPort port;
        private long value;

        Entry(long sw, OFPort port, long value) {
            this.sw = sw;
            this.port = port;
            this.value = value;
        }

        public OFPort getPort() {
            return port;
        }

        public long getSw() {
            return sw;
        }

        public long getValue() {
            return value;
        }
    }

    private class InEntry extends Entry {
        InEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }

        InEntry(OutEntry outEntry, long value) {
            super(outEntry.getSw(),outEntry.getPort(), value);
        }
    }

    private class OutEntry extends Entry {
        OutEntry(long sw, OFPort port, long value) {
            super(sw, port, value);
        }
    }

    public void addHost(long sw, OFPort p){
        learned.put(new InEntry(sw, p, SaraProtocolUtils.TIME_OUT), new OutEntry(sw, p, SaraProtocolUtils.TIME_OUT));
    }

    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long value) {
        if (learned.containsKey(new InEntry(sw, inPort,value)))
            return;
        else if (waitingRoom.containsKey(sw)) {
            for( OutEntry current : waitingRoom.get(sw)){
                if(current.getSw() == currentSwitch){
//                    learned.put(new InEntry(sw,inPort,value),current);
                    learned.put(new InEntry(current,value),new OutEntry(sw,inPort,value));
                }
            }
            waitingRoom.get(sw).add(new OutEntry(sw, inPort,value));
        }else {
            Set<OutEntry> temp = new HashSet<>();
            temp.add(new OutEntry(sw, inPort,value));
            waitingRoom.put(currentSwitch,temp);
        }
    }

    class Graph {
        class Edge implements Comparable<Edge> {
            long weight;
            InEntry src;
            OutEntry dest;

            public int compareTo(Edge compareEdge) {
                return (int)(this.weight - compareEdge.weight);
            }
        }

        ;

        class subset {
            int parent, rank;
        }

        ;

        int V, E;
        Edge edge[];

        Graph(int v, int e) {
            V = v;
            E = e;
            edge = new Edge[E];
            for (int i = 0; i < e; ++i)
                edge[i] = new Edge();
        }

        int find(subset subsets[], int i) {
            if (subsets[i].parent != i)
                subsets[i].parent = find(subsets, subsets[i].parent);

            return subsets[i].parent;
        }

        void Union(subset subsets[], int x, int y) {
            int xroot = find(subsets, x);
            int yroot = find(subsets, y);

            if (subsets[xroot].rank < subsets[yroot].rank)
                subsets[xroot].parent = yroot;
            else if (subsets[xroot].rank > subsets[yroot].rank)
                subsets[yroot].parent = xroot;



            else {
                subsets[yroot].parent = xroot;
                subsets[xroot].rank++;
            }
        }

        Edge[] KruskalMST() {
            Edge result[] = new Edge[V];
            int e = 0;
            int i = 0;
            for (i = 0; i < V; ++i)
                result[i] = new Edge();

            Arrays.sort(edge);

            subset subsets[] = new subset[V];
            for (i = 0; i < V; ++i)
                subsets[i] = new subset();

            for (int v = 0; v < V; ++v) {
                subsets[v].parent = v;
                subsets[v].rank = 0;
            }

            i = 0;

            while (e < V - 1) {
                Edge next_edge = new Edge();
                next_edge = edge[i++];

                int x = find(subsets, (int) next_edge.src.getSw());
                int y = find(subsets, (int) next_edge.dest.getSw());

                if (x != y) {
                    result[e++] = next_edge;
                    Union(subsets, x, y);
                }
            }
            return result;
        }
    }
}
