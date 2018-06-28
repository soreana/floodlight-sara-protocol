package net.floodlightcontroller.learningswitch;

import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SaraProtocol {
    protected static Logger log = LoggerFactory.getLogger(LearningSwitch.class);

    private Map<Long, Switch> switches = new HashMap<>();

    private long currentSwitch;
    long broadcastStart = 0;
    long newARPTime = 0;

    private long routeSwitch;
    private OFPort routePort;

    public void setRouteSwitch(long routeSwitch) {
        this.routeSwitch = routeSwitch;
    }

    public void setRoutePort(OFPort routePort) {
        this.routePort = routePort;
    }

    public void markHost() {
        if (routePort != OFPort.ZERO) {
            switches.get(routeSwitch).adjacents.put(routePort, new Entry(true));
            log.info("host detected: switch {} port {}", routeSwitch, routePort.getPortNumber());
        }
    }

    public void addMe(long id) {
        if (!switches.containsKey(id)) {
            switches.put(id, new Switch(id));
        }
    }

    public void setCurrentSwitch(long currentSwitch) {
        this.currentSwitch = currentSwitch;
        switches.get(currentSwitch).setDidBroadcast(true);
    }

    void markCurrentSwitchPort(OFPort p) {
        Switch s = switches.get(currentSwitch);
        if (!s.adjacents.containsKey(p)) {
            switches.get(currentSwitch).adjacents.put(p, null);
        }
    }

    private OFPort findRoutePortRecursively(Switch curr) {
        Switch last = null;
        while (curr.father != -1) {
            last = curr;
            curr = switches.get(curr.father);
        }
        if (last != null) {
            for (OFPort p : curr.adjacents.keySet()) {
                Entry e = curr.adjacents.get(p);
                if (e != null && e.getSw() == last.id) {
                    return p;
                }
            }
        }
        return null;
    }

    public OFPort getRoutePort(long switchId) {
        Switch sw = switches.get(switchId);
        if (sw.getDidBroadcast()) {
            for (long sid : switches.keySet()) {
                switches.get(sid).init();
            }
            sw.dist = 0;
            Switch min = sw;
            while (true) {
                if (!min.getDidBroadcast()) {
                    return findRoutePortRecursively(min);
                }
                for (OFPort p : min.adjacents.keySet()) {
                    Entry e = min.adjacents.get(p);
                    if (e == null) {
                        // Link is not recognized
                        log.info("Link is not recognized");
                        OFPort ans = findRoutePortRecursively(min);
                        if (ans == null) {
                            return p;
                        }
                        else {
                            return ans;
                        }
                    }
                    else if (!e.getIsHost()) {
                        Switch s = switches.get(e.getSw());
                        if (s.isFinal) {
                            continue;
                        }
                        if (s.dist == -1 || s.dist > min.dist + e.getDist()) {
                            s.dist = min.dist + e.getDist();
                            s.father = min.id;
                        }
                    }
                }
                min.isFinal = true;
                long minDist = -1;
                for (long sid : switches.keySet()) {
                    Switch s = switches.get(sid);
                    if (!s.isFinal && s.dist != -1 && (minDist == -1 || s.dist < minDist)) {
                        minDist = s.dist;
                        min = s;
                    }
                }
            }
        }
        else {
            return OFPort.ZERO;
        }
    }

    void printMST() {
        long switchId = 0;
        for (Iterator<Long> it = switches.keySet().iterator(); it.hasNext(); ) {
            switchId = it.next();
            break;
        }
        Switch sw = switches.get(switchId);
        for (long sid : switches.keySet()) {
            switches.get(sid).init();
        }
        sw.isFinal = true;
        Set<Switch> visited = new HashSet<>();
        visited.add(sw);
        Switch u=null, v=null;
        while (visited.size() < switches.size()) {
            long minDist = -1;
            for (Switch s : visited) {
                for (OFPort p : s.adjacents.keySet()) {
                    Entry e = s.adjacents.get(p);
                    if (e.getIsHost()) {
                        continue;
                    }
                    Switch s2 = switches.get(e.getSw());
                    if (!s2.isFinal) {
                        if (minDist == -1 || e.getDist() < minDist) {
                            minDist = e.getDist();
                            u = s;
                            v = s2;
                        }
                    }
                }
            }
            v.isFinal = true;
            visited.add(v);
            log.info("switch "+u.id+" <-> switch "+v.id);
        }
    }

    private class Switch {
        private long id;
        private Map<OFPort, Entry> adjacents;
        private boolean didBroadcast;

        public long father;
        public long dist;
        public boolean isFinal;

        Switch(long id) {
            this.id = id;
            didBroadcast = false;
            adjacents = new HashMap<>();
        }

        boolean getDidBroadcast() {
            return didBroadcast;
        }

        void setDidBroadcast(boolean didBroadcast) {
            this.didBroadcast = didBroadcast;
        }

        void init() {
            this.dist = -1;
            this.father = -1;
            this.isFinal = false;
        }
    }

    private class Entry {
        private long sw = 0;
        private OFPort port = null;
        private long dist = 0;
        private boolean isHost = false;

        Entry(long sw, OFPort port, long dist) {
            this.sw = sw;
            this.port = port;
            this.dist = dist;
        }

        Entry(boolean isHost) {
            this.isHost = isHost;
        }

        public long getSw() {
            return sw;
        }

        public void setSw(long sw) {
            this.sw = sw;
        }

        public OFPort getPort() {
            return port;
        }

        public void setPort(OFPort port) {
            this.port = port;
        }

        public long getDist() {
            return dist;
        }

        public void setDist(long dist) {
            this.dist = dist;
        }

        public boolean getIsHost() {
            return isHost;
        }
    }

    public void learnLinkForCurrentSwitch(long sw, OFPort inPort, long dist) {
        if (sw == currentSwitch) {
            // The link is connected to another host
            log.info("This packet came from the other host!");
            switches.get(sw).adjacents.put(inPort, new Entry(true));
        }
        else {
            Switch next = switches.get(sw);
            Switch curr = switches.get(currentSwitch);
            Entry currEntry = new Entry(currentSwitch, null, dist);
            long newDist = dist;
            for (OFPort p : curr.adjacents.keySet()) {
                Entry adjacent = curr.adjacents.get(p);
                if (adjacent != null && adjacent.getSw() == sw) {
                    if (adjacent.getDist() < newDist) {
                        newDist = adjacent.getDist();
                    }
                    currEntry.setPort(p);
                    adjacent.setPort(inPort);
                    adjacent.setDist(newDist);
                }
            }
            currEntry.setDist(newDist);
            next.adjacents.put(inPort, currEntry);
        }
        checkFinish();
    }

    private void checkFinish() {
        for (long sw : switches.keySet()) {
            Switch s = switches.get(sw);
            for (OFPort p : s.adjacents.keySet()) {
                Entry e = s.adjacents.get(p);
                if (e == null || (!e.getIsHost() && (e.getPort() == null || e.getSw() == 0))) {
                    return;
                }
            }
        }
        log.info("----------------------------------------------------------------------");
        log.info("--------------------------- TOPOLOGY FOUND ---------------------------");
        log.info("----------------------------------------------------------------------");
        printTopology();
        log.info("----------------------------------------------------------------------");
        log.info("----------------------------- MST EDGES ------------------------------");
        log.info("----------------------------------------------------------------------");
        printMST();
        log.info("----------------------------------------------------------------------");
        log.info("-------------------------------- DONE --------------------------------");
        log.info("----------------------------------------------------------------------");
    }

    private void printTopology() {
        for (long sw : switches.keySet()) {
            Switch s = switches.get(sw);
            log.info("Switch {}:", sw);
            for (OFPort p : s.adjacents.keySet()) {
                Entry e = s.adjacents.get(p);
                if (e == null) {
                    log.info("--> Port {} is null", p.getPortNumber());
                }
                else if (e.getIsHost()) {
                    log.info("--> Port {} is connected to host", p.getPortNumber());
                }
                else {
                    log.info("--> Port "+String.valueOf(p.getPortNumber())+" is connected to port "+String.valueOf(e.getPort())+" of switch "+String.valueOf(e.getSw())+" ("+String.valueOf(e.getDist())+"ms)");
                }
            }
        }
    }
}
